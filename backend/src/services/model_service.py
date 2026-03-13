import os
import json
from dotenv import load_dotenv

load_dotenv()

class ModelService:
    _instance = None
    model = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._load_model()
        return cls._instance
    
    def _load_model(self):
        try:
            model_path = os.getenv("MODEL_PATH", "./models/best_model.pt")
            
            if os.path.exists(model_path):
                try:
                    from ultralytics import YOLO
                    self.model = YOLO(model_path)
                    print("YOLOv8模型加载成功")
                except ImportError:
                    print("ultralytics库未安装，使用模拟模型")
                    self.model = None
                except Exception as e:
                    print(f"YOLOv8模型加载失败: {e}")
                    self.model = None
            else:
                print(f"模型文件不存在: {model_path}，使用模拟模型")
                self.model = None
        except Exception as e:
            print(f"模型加载异常: {e}")
            self.model = None
    
    def _get_nutrition_data(self, fruit_name: str):
        nutrition_db = {
            "番茄": {
                "calories": "18kcal",
                "water": "95g",
                "protein": "0.9g",
                "vitamin_c": "19mg",
                "tips": "番茄熟吃更利于番茄红素吸收"
            },
            "黄瓜": {
                "calories": "16kcal",
                "water": "96g",
                "protein": "0.8g",
                "vitamin_c": "9mg",
                "tips": "黄瓜皮富含营养，建议带皮食用"
            },
            "苹果": {
                "calories": "52kcal",
                "water": "86g",
                "protein": "0.3g",
                "vitamin_c": "4.6mg",
                "tips": "苹果富含膳食纤维，有益肠道健康"
            },
            "香蕉": {
                "calories": "89kcal",
                "water": "75g",
                "protein": "1.1g",
                "vitamin_c": "8.7mg",
                "tips": "香蕉富含钾，有助于维持血压"
            },
            "橙子": {
                "calories": "47kcal",
                "water": "87g",
                "protein": "0.9g",
                "vitamin_c": "53.2mg",
                "tips": "橙子是维生素C的优质来源"
            },
            "模拟预测结果": {
                "calories": "50kcal",
                "water": "85g",
                "protein": "0.5g",
                "vitamin_c": "10mg",
                "tips": "这是模拟的营养数据"
            }
        }
        return nutrition_db.get(fruit_name, {
            "calories": "50kcal",
            "water": "85g",
            "protein": "0.5g",
            "vitamin_c": "10mg",
            "tips": "营养数据待完善"
        })
    
    async def predict(self, image_path: str):
        if self.model is not None:
            try:
                results = self.model(image_path)
                
                if len(results) == 0:
                    return None
                
                result = results[0]
                
                if len(result.boxes) == 0:
                    return None
                
                box = result.boxes[0]
                class_id = int(box.cls[0])
                confidence = float(box.conf[0])
                label = result.names[class_id]
                
                nutrition_data = self._get_nutrition_data(label)
                
                return {
                    "class_id": class_id,
                    "confidence": confidence,
                    "label": label,
                    "nutrition_data": nutrition_data
                }
            except Exception as e:
                print(f"YOLOv8预测失败，使用模拟预测: {e}")
        
        return {
            "class_id": 0,
            "confidence": 0.95,
            "label": "模拟预测结果",
            "nutrition_data": self._get_nutrition_data("模拟预测结果"),
            "message": "使用模拟预测，YOLOv8模型未加载"
        }
