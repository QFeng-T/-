#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
YOLOv8 果蔬目标检测训练脚本
使用 LVIS_Fruits_And_Vegetables 数据集训练 YOLOv8 模型

功能:
1. 加载 YOLOv8 预训练模型
2. 配置训练参数
3. 启动训练流程
4. 保存训练结果和模型
5. 支持模型导出
"""

import os
import sys
import yaml
import torch
import argparse
import logging
from pathlib import Path
from datetime import datetime
from typing import Dict, Any

try:
    from ultralytics import YOLO
    YOLO_AVAILABLE = True
except ImportError:
    print("错误: ultralytics 未安装，请运行: pip install ultralytics")
    sys.exit(1)

# 设置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

def load_config(config_path: str) -> Dict[str, Any]:
    """
    加载配置文件
    
    Args:
        config_path: 配置文件路径
        
    Returns:
        配置字典
    """
    logger.info(f"加载配置文件: {config_path}")
    
    with open(config_path, 'r', encoding='utf-8') as f:
        config = yaml.safe_load(f)
    
    return config

def check_gpu() -> bool:
    """
    检查 GPU 是否可用
    
    Returns:
        GPU 是否可用
    """
    if torch.cuda.is_available():
        gpu_name = torch.cuda.get_device_name(0)
        gpu_memory = torch.cuda.get_device_properties(0).total_memory / 1024**3
        logger.info(f"检测到 GPU: {gpu_name}")
        logger.info(f"GPU 显存: {gpu_memory:.1f} GB")
        
        # RTX 3060 特殊优化
        if '3060' in gpu_name:
            logger.info("⚡ 检测到 RTX 3060，启用显存优化模式")
            torch.cuda.empty_cache()
        
        return True
    else:
        logger.warning("⚠️  未检测到 GPU，将使用 CPU 训练（速度较慢）")
        return False

def train_yolov8(config: Dict[str, Any]):
    """
    训练 YOLOv8 模型
    
    Args:
        config: 配置字典
    """
    # 检查 GPU
    has_gpu = check_gpu()
    device = 'cuda' if has_gpu else 'cpu'
    
    # 检查 wandb
    use_wandb = config.get('use_wandb', False)
    if use_wandb:
        try:
            import wandb
            logger.info("✅ wandb 已安装，启用实验跟踪")
            # 初始化 wandb
            wandb.init(
                project=config.get('project_name', 'yolov8-fruit-detection'),
                name=config.get('experiment_name', 'yolov8n_training'),
                config=config
            )
        except ImportError:
            logger.warning("⚠️  wandb 未安装，将禁用实验跟踪")
            logger.info("  请运行: pip install wandb 来启用 wandb 功能")
            use_wandb = False
    else:
        logger.info("ℹ️  未启用 wandb 实验跟踪")
    
    # 创建输出目录
    output_dir = Path(config['output_dir'])
    output_dir.mkdir(parents=True, exist_ok=True)
    logger.info(f"输出目录: {output_dir}")
    
    # 获取模型类型
    model_type = config['model']['model_type']
    pretrained = config['model']['pretrained']
    logger.info(f"使用模型: {model_type}")
    logger.info(f"预训练权重: {pretrained}")
    
    # 加载 YOLOv8 模型
    if pretrained:
        # 检查本地模型文件
        local_model_path = Path(__file__).parent / f"{model_type}.pt"
        if local_model_path.exists():
            model = YOLO(str(local_model_path))
            logger.info(f"加载本地预训练模型: {local_model_path}")
        else:
            model = YOLO(f"{model_type}.pt")
            logger.info(f"加载预训练模型: {model_type}.pt")
    else:
        model = YOLO(f"{model_type}.yaml")
        logger.info(f"创建新模型: {model_type}.yaml")
    
    # 数据集配置
    data_yaml = config['data']['data_yaml']
    logger.info(f"数据集配置: {data_yaml}")
    
    # 构建训练参数
    training_config = config['training']
    augmentation_config = config['augmentation']
    
    # 训练参数 - 包含详细的监测和保存配置
    train_params = {
        'data': data_yaml,
        'epochs': training_config['epochs'],
        'batch': config['data']['batch_size'],
        'imgsz': config['data']['image_size'],
        'device': device,
        'workers': config['data']['num_workers'],
        'cache': config['data']['cache'],
        'project': str(output_dir),
        'name': config['experiment_name'],
        'exist_ok': True,
        'pretrained': pretrained,
        'optimizer': 'auto',
        'lr0': training_config['learning_rate'],
        'lrf': training_config['min_lr'] / training_config['learning_rate'],
        'momentum': training_config['momentum'],
        'weight_decay': training_config['weight_decay'],
        'warmup_epochs': training_config['warmup_epochs'],
        'warmup_bias_lr': training_config['warmup_bias_lr'],
        'patience': training_config['patience'],
        'save_period': training_config['save_period'],
        'cos_lr': training_config['lr_scheduler'] == 'cos',
        # 模型保存配置 - 保留最佳和最后模型
        'save': True,
        # 详细的监测和输出
        'verbose': True,
        'plots': True,
        'save_json': True,
        'save_txt': True,
        'save_conf': True,
        'save_crop': True,
        # 验证配置
        'val': True,
        # 数据增强
        'hsv_h': augmentation_config['hsv_h'],
        'hsv_s': augmentation_config['hsv_s'],
        'hsv_v': augmentation_config['hsv_v'],
        'degrees': augmentation_config['degrees'],
        'translate': augmentation_config['translate'],
        'scale': augmentation_config['scale'],
        'shear': augmentation_config['shear'],
        'perspective': augmentation_config['perspective'],
        'flipud': augmentation_config['flipud'],
        'fliplr': augmentation_config['fliplr'],
        'mosaic': augmentation_config['mosaic'],
        'mixup': augmentation_config['mixup'],
        'copy_paste': augmentation_config['copy_paste'],
        # RTX 3060 优化
        'amp': config['rtx3060_optimization']['amp'],
    }
    
    # 打印训练参数
    logger.info("=" * 60)
    logger.info("🚀 开始 YOLOv8 训练")
    logger.info("=" * 60)
    logger.info(f"训练参数:")
    for key, value in train_params.items():
        if key not in ['data', 'project', 'name']:
            logger.info(f"  - {key}: {value}")
    
    # 开始训练
    try:
        results = model.train(**train_params)
        
        logger.info("=" * 60)
        logger.info("✅ 训练完成！")
        logger.info("=" * 60)
        
        # 训练输出目录
        run_dir = output_dir / config['experiment_name']
        weights_dir = run_dir / 'weights'
        
        # 检查并确认模型文件
        logger.info("\n📁 检查训练输出文件：")
        
        # 最佳模型
        best_model_path = weights_dir / 'best.pt'
        if best_model_path.exists():
            best_size_mb = best_model_path.stat().st_size / (1024 * 1024)
            logger.info(f"✅ 最佳模型: {best_model_path} ({best_size_mb:.2f} MB)")
        else:
            logger.warning("⚠️  未找到 best.pt 文件")
        
        # 最后模型
        last_model_path = weights_dir / 'last.pt'
        if last_model_path.exists():
            last_size_mb = last_model_path.stat().st_size / (1024 * 1024)
            logger.info(f"✅ 最后模型: {last_model_path} ({last_size_mb:.2f} MB)")
        else:
            logger.warning("⚠️  未找到 last.pt 文件")
        
        # 检查CSV输出文件
        csv_path = run_dir / 'results.csv'
        if csv_path.exists():
            csv_size_mb = csv_path.stat().st_size / (1024 * 1024)
            logger.info(f"✅ 训练结果CSV: {csv_path} ({csv_size_mb:.2f} MB)")
            
            # 读取CSV文件并显示前几行，方便分析
            logger.info("\n📊 CSV文件内容预览：")
            try:
                import pandas as pd
                df = pd.read_csv(csv_path)
                logger.info(f"   数据行数: {len(df)}")
                logger.info(f"   列名: {list(df.columns)}")
                logger.info("\n   前5行数据:")
                print(df.head().to_string())
            except ImportError:
                logger.info("   (pandas未安装，无法预览，请在WSL2中运行: pip install pandas)")
            except Exception as e:
                logger.warning(f"   读取CSV预览时出错: {e}")
        else:
            logger.warning("⚠️  未找到 results.csv 文件")
        
        # 检查图表文件
        plot_files = ['results.png', 'F1_curve.png', 'PR_curve.png', 'P_curve.png', 'R_curve.png', 
                     'confusion_matrix.png', 'labels.jpg', 'labels_correlogram.jpg']
        logger.info("\n📈 训练图表文件：")
        for plot_file in plot_files:
            plot_path = run_dir / plot_file
            if plot_path.exists():
                logger.info(f"   ✅ {plot_file}")
            else:
                logger.info(f"   ⚠️  {plot_file} (未生成)")
        
        # 模型导出
        export_config = config['export']
        if export_config['formats']:
            logger.info(f"\n🚀 开始导出模型...")
            for export_format in export_config['formats']:
                try:
                    logger.info(f"   导出格式: {export_format}")
                    export_path = model.export(
                        format=export_format,
                        imgsz=export_config['input_size'],
                        simplify=export_config['simplify'],
                        half=export_config['half_precision']
                    )
                    logger.info(f"   ✅ 导出成功: {export_path}")
                except Exception as e:
                    logger.error(f"   ❌ 导出失败 ({export_format}): {e}")
        
        logger.info("\n" + "=" * 60)
        logger.info("📋 训练输出总结：")
        logger.info("=" * 60)
        logger.info(f"输出目录: {run_dir}")
        logger.info(f"最佳模型: {best_model_path if best_model_path.exists() else '未找到'}")
        logger.info(f"最后模型: {last_model_path if last_model_path.exists() else '未找到'}")
        logger.info(f"训练日志CSV: {csv_path if csv_path.exists() else '未找到'}")
        logger.info("=" * 60)
        
        return results
        
    except Exception as e:
        logger.error(f"❌ 训练失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

def main():
    """
    主函数
    """
    parser = argparse.ArgumentParser(description='YOLOv8 果蔬目标检测训练')
    parser.add_argument('--config', '-c', required=True, help='配置文件路径')
    
    args = parser.parse_args()
    
    # 加载配置
    config = load_config(args.config)
    
    # 训练 YOLOv8
    train_yolov8(config)

if __name__ == '__main__':
    main()
