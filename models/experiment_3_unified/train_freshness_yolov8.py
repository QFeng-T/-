#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
YOLOv8 新鲜度检测训练脚本
使用 fruit_freshness_dataset 数据集训练 YOLOv8 模型
用于同时检测果蔬类别和新鲜度（126个类别）
"""

import os
import sys
import yaml
import json
import torch
import logging
import time
from pathlib import Path
from datetime import datetime
from typing import Dict, Any, Optional

try:
    from ultralytics import YOLO
    YOLO_AVAILABLE = True
except ImportError:
    print("错误: ultralytics 未安装，请运行: pip install ultralytics")
    sys.exit(1)

# 尝试导入 wandb
WANDB_AVAILABLE = False
try:
    import wandb
    WANDB_AVAILABLE = True
except ImportError:
    print("提示: wandb 未安装，如需实验跟踪请运行: pip install wandb")

# 设置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class TrainingUI:
    """美观的训练界面显示类"""
    
    @staticmethod
    def print_header(title: str):
        """打印美观的标题"""
        width = 80
        print("\n" + "╔" + "═" * (width - 2) + "╗")
        print(f"║ {title.center(width - 4)} ║")
        print("╚" + "═" * (width - 2) + "╝")
    
    @staticmethod
    def print_section(title: str):
        """打印分区标题"""
        width = 80
        print(f"\n{'─' * 20} {title} {'─' * (width - 22 - len(title))}")
    
    @staticmethod
    def print_info(key: str, value: Any, indent: int = 0):
        """打印信息键值对"""
        indent_str = "  " * indent
        print(f"{indent_str}📌 {key:<25} : {value}")
    
    @staticmethod
    def print_success(message: str):
        """打印成功消息"""
        print(f"✅ {message}")
    
    @staticmethod
    def print_warning(message: str):
        """打印警告消息"""
        print(f"⚠️  {message}")
    
    @staticmethod
    def print_error(message: str):
        """打印错误消息"""
        print(f"❌ {message}")

class TrainingInfoSaver:
    """训练信息保存类"""
    
    def __init__(self, output_dir: Path):
        self.output_dir = output_dir
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.training_info = {
            'start_time': datetime.now().isoformat(),
            'end_time': None,
            'config': {},
            'results': {},
            'metrics': []
        }
    
    def save_config(self, config: Dict[str, Any]):
        """保存训练配置"""
        self.training_info['config'] = config
        
        # 保存为 YAML
        yaml_path = self.output_dir / 'training_config.yaml'
        with open(yaml_path, 'w', encoding='utf-8') as f:
            yaml.dump(config, f, default_flow_style=False, allow_unicode=True)
        
        # 保存为 JSON
        json_path = self.output_dir / 'training_config.json'
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(config, f, indent=2, ensure_ascii=False)
        
        TrainingUI.print_success(f"配置已保存: {yaml_path.name}")
    
    def save_results(self, results: Any, run_dir: Path):
        """保存训练结果"""
        self.training_info['end_time'] = datetime.now().isoformat()
        
        # 收集结果信息
        if hasattr(results, 'box'):
            self.training_info['results'] = {
                'mAP50': float(results.box.map50) if hasattr(results.box, 'map50') else None,
                'mAP50_95': float(results.box.map) if hasattr(results.box, 'map') else None,
                'precision': float(results.box.mp) if hasattr(results.box, 'mp') else None,
                'recall': float(results.box.mr) if hasattr(results.box, 'mr') else None,
            }
        
        # 读取 CSV 结果
        csv_path = run_dir / 'results.csv'
        if csv_path.exists():
            try:
                import pandas as pd
                df = pd.read_csv(csv_path)
                self.training_info['metrics'] = df.to_dict('records')
            except ImportError:
                TrainingUI.print_warning("pandas 未安装，无法读取 CSV 详细数据")
        
        # 保存完整信息
        yaml_path = self.output_dir / 'training_results.yaml'
        with open(yaml_path, 'w', encoding='utf-8') as f:
            yaml.dump(self.training_info, f, default_flow_style=False, allow_unicode=True)
        
        json_path = self.output_dir / 'training_results.json'
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(self.training_info, f, indent=2, ensure_ascii=False)
        
        TrainingUI.print_success(f"训练结果已保存: {yaml_path.name}")

def check_gpu(ui: TrainingUI) -> bool:
    """检查 GPU 是否可用"""
    ui.print_section("硬件检测")
    
    if torch.cuda.is_available():
        gpu_name = torch.cuda.get_device_name(0)
        gpu_memory = torch.cuda.get_device_properties(0).total_memory / 1024 ** 3
        gpu_memory_allocated = torch.cuda.memory_allocated() / 1024 ** 3
        
        ui.print_info("GPU 型号", gpu_name)
        ui.print_info("GPU 显存", f"{gpu_memory:.1f} GB")
        ui.print_info("已用显存", f"{gpu_memory_allocated:.2f} GB")
        
        if '3060' in gpu_name:
            ui.print_success("检测到 RTX 3060，启用显存优化模式")
            torch.cuda.empty_cache()
        
        return True
    else:
        ui.print_warning("未检测到 GPU，将使用 CPU 训练（速度较慢）")
        return False

def train_yolov8_freshness():
    """训练 YOLOv8 新鲜度检测模型"""
    ui = TrainingUI()
    
    # 配置参数
    MODEL_PATH = r"/mnt/d/Desktop/FreshID/models/experiment_3_unified/results/freshness_yolov8_v1/weights/best.pt"
    DATA_YAML = r"/home/th/data/unified_freshness/data.yaml"
    OUTPUT_DIR = r"/mnt/d/Desktop/FreshID/models/experiment_3_unified/results"
    
    ui.print_header("YOLOv8 新鲜度检测训练")
    
    # 检查文件
    ui.print_section("文件检查")
    
    if not Path(MODEL_PATH).exists():
        ui.print_error(f"模型文件不存在: {MODEL_PATH}")
        sys.exit(1)
    ui.print_info("预训练模型", Path(MODEL_PATH).name)
    
    if not Path(DATA_YAML).exists():
        ui.print_error(f"数据集配置文件不存在: {DATA_YAML}")
        ui.print_info("提示", "请先运行 change.py 生成标注数据集")
        sys.exit(1)
    ui.print_info("数据集配置", Path(DATA_YAML).name)
    
    # 检查 GPU
    has_gpu = check_gpu(ui)
    device = 'cuda' if has_gpu else 'cpu'
    
    # 创建输出目录
    output_dir = Path(OUTPUT_DIR)
    output_dir.mkdir(parents=True, exist_ok=True)
    ui.print_info("输出目录", str(output_dir))
    
    # 初始化信息保存器
    saver = TrainingInfoSaver(output_dir / f"training_info_{datetime.now().strftime('%Y%m%d_%H%M%S')}")
    
    # 加载 YOLOv8 模型
    ui.print_section("模型加载")
    ui.print_info("正在加载模型", Path(MODEL_PATH).name)
    
    try:
        model = YOLO(MODEL_PATH)
        ui.print_success("模型加载成功")
    except Exception as e:
        ui.print_error(f"模型加载失败: {e}")
        sys.exit(1)
    
    # 读取 data.yaml 获取类别信息
    ui.print_section("数据集信息")
    with open(DATA_YAML, 'r', encoding='utf-8') as f:
        data_config = yaml.safe_load(f)
    
    num_classes = data_config.get('nc', 126)
    class_names = data_config.get('names', {})
    
    ui.print_info("类别总数", num_classes)
    ui.print_info("前5个类别", ", ".join(list(class_names.values())[:5]))
    
    # 训练参数配置
    ui.print_section("训练参数")
    
    train_params = {
        'data': DATA_YAML,
        'epochs': 100,
        'batch': 16,
        'imgsz': 640,
        'device': device,
        'workers': 4,
        'cache': 'disk',
        'project': str(output_dir),
        'name': 'freshness_yolov8_v2',
        'exist_ok': True,
        'pretrained': True,
        'optimizer': 'AdamW',
        'lr0': 0.0005,
        'lrf': 0.001,
        'momentum': 0.937,
        'weight_decay': 0.0005,
        'warmup_epochs': 3,
        'warmup_bias_lr': 0.1,
        'patience': 20,
        'save_period': 5,
        'cos_lr': True,
        'save': True,
        'verbose': False,  # 关闭默认的逐行输出
        'plots': True,
        'save_json': True,
        'save_txt': True,
        'val': True,
        'hsv_h': 0.015,
        'hsv_s': 0.7,
        'hsv_v': 0.4,
        'degrees': 10,
        'translate': 0.1,
        'scale': 0.5,
        'shear': 0.0,
        'perspective': 0.0,
        'flipud': 0.0,
        'fliplr': 0.5,
        'mosaic': 0.8,
        'mixup': 0.1,
        'copy_paste': 0.0,
        'amp': True,
    }
    
    # 显示关键训练参数
    ui.print_info("训练轮数", train_params['epochs'])
    ui.print_info("批次大小", train_params['batch'])
    ui.print_info("图像尺寸", train_params['imgsz'])
    ui.print_info("初始学习率", train_params['lr0'])
    ui.print_info("优化器", train_params['optimizer'])
    ui.print_info("学习率调度", "余弦退火" if train_params['cos_lr'] else "线性")
    ui.print_info("预热轮数", train_params['warmup_epochs'])
    ui.print_info("早停耐心值", train_params['patience'])
    ui.print_info("数据增强", f"Mosaic={train_params['mosaic']}, MixUp={train_params['mixup']}")
    
    # 保存配置
    saver.save_config(train_params)
    
    # 初始化 wandb
    wandb_run = None
    if WANDB_AVAILABLE:
        ui.print_section("WandB 初始化")
        try:
            wandb_run = wandb.init(
                project="yolov8-freshness",
                name=f"freshness_yolov8_{datetime.now().strftime('%Y%m%d_%H%M%S')}",
                config=train_params,
                reinit=True
            )
            ui.print_success("WandB 初始化成功")
            ui.print_info("项目名称", "yolov8-freshness")
            ui.print_info("实验名称", wandb_run.name)
        except Exception as e:
            ui.print_warning(f"WandB 初始化失败: {e}")
            wandb_run = None
    
    # 开始训练
    ui.print_header("开始训练")
    ui.print_info("开始时间", datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
    ui.print_info("训练模式", "实时进度条模式")
    ui.print_info("进度显示", "同一行实时更新")
    
    try:
        # 训练前提示
        print("\n🔄 训练中... (按 Ctrl+C 停止)")
        print("=" * 80)
        
        # 开始训练
        print("训练过程中，实时更新进度...")
        print("=" * 80)
        
        # 定义实时进度更新函数
        def update_progress(epoch, total_epochs, loss=None):
            """更新实时进度条"""
            progress = (epoch / total_epochs) * 100
            bar_length = 50
            filled_length = int(bar_length * epoch / total_epochs)
            bar = '█' * filled_length + '-' * (bar_length - filled_length)
            
            if loss:
                status = f"Epoch {epoch}/{total_epochs} | Loss: {loss:.4f} | {progress:.1f}%"
            else:
                status = f"Epoch {epoch}/{total_epochs} | {progress:.1f}%"
            
            # 使用回车符实现同一行更新
            sys.stdout.write(f'\r[{bar}] {status}')
            sys.stdout.flush()
        
        # 开始训练
        total_epochs = train_params['epochs']
        
        # 这里我们无法直接监控内部训练过程
        # 但我们可以显示一个进度指示器
        print("正在启动训练...")
        
        # 启动训练（这是一个阻塞调用）
        results = model.train(**train_params)
        
        # 训练完成后更新进度到100%
        update_progress(total_epochs, total_epochs)
        print("\n")
        
        ui.print_header("训练完成")
        ui.print_info("结束时间", datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
        
        # 训练输出目录
        run_dir = output_dir / 'freshness_yolov8_v2'
        weights_dir = run_dir / 'weights'
        
        ui.print_section("训练输出")
        
        # 检查并确认模型文件
        best_model_path = weights_dir / 'best.pt'
        if best_model_path.exists():
            best_size_mb = best_model_path.stat().st_size / (1024 * 1024)
            ui.print_info("最佳模型", f"{best_model_path.name} ({best_size_mb:.2f} MB)")
            ui.print_success("最佳模型已保存")
        else:
            ui.print_warning("未找到 best.pt 文件")
        
        last_model_path = weights_dir / 'last.pt'
        if last_model_path.exists():
            last_size_mb = last_model_path.stat().st_size / (1024 * 1024)
            ui.print_info("最后模型", f"{last_model_path.name} ({last_size_mb:.2f} MB)")
        else:
            ui.print_warning("未找到 last.pt 文件")
        
        # 检查图表文件
        plot_files = [
            'results.png', 'F1_curve.png', 'PR_curve.png', 
            'P_curve.png', 'R_curve.png', 'confusion_matrix.png',
            'labels.jpg', 'labels_correlogram.jpg'
        ]
        
        ui.print_section("可视化图表")
        for plot_file in plot_files:
            plot_path = run_dir / plot_file
            if plot_path.exists():
                ui.print_success(plot_file)
            else:
                ui.print_warning(f"{plot_file} (未生成)")
        
        # 保存训练结果
        saver.save_results(results, run_dir)
        
        # 上传图表到 wandb
        if wandb_run is not None:
            ui.print_section("WandB 上传")
            try:
                for plot_file in plot_files:
                    plot_path = run_dir / plot_file
                    if plot_path.exists():
                        wandb.log({plot_file: wandb.Image(str(plot_path))})
                
                if best_model_path.exists():
                    wandb.save(str(best_model_path))
                
                ui.print_success("图表和模型已上传到 WandB")
            except Exception as e:
                ui.print_warning(f"WandB 上传失败: {e}")
        
        ui.print_section("总结")
        ui.print_info("输出目录", str(run_dir))
        ui.print_info("最佳模型", str(best_model_path) if best_model_path.exists() else "未找到")
        ui.print_info("配置文件", str(saver.output_dir / 'training_config.yaml'))
        ui.print_info("结果文件", str(saver.output_dir / 'training_results.yaml'))
        
        ui.print_header("训练成功完成！")
        
        return results
        
    except KeyboardInterrupt:
        ui.print_warning("训练被用户中断")
        if wandb_run is not None:
            wandb_run.finish()
        sys.exit(1)
    except Exception as e:
        ui.print_error(f"训练失败: {e}")
        import traceback
        traceback.print_exc()
        if wandb_run is not None:
            wandb_run.finish()
        sys.exit(1)
    finally:
        if wandb_run is not None:
            wandb_run.finish()

def main():
    """主函数"""
    train_yolov8_freshness()

if __name__ == '__main__':
    main()