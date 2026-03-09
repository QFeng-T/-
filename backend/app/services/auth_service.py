import os
import random
import string
from datetime import datetime, timedelta
from typing import Optional, Tuple
from dotenv import load_dotenv
from jose import JWTError, jwt
from passlib.context import CryptContext

load_dotenv()

class AuthService:
    def __init__(self):
        self.secret_key = os.getenv("JWT_SECRET_KEY", "your-secret-key-change-in-production")
        self.algorithm = os.getenv("JWT_ALGORITHM", "HS256")
        self.access_token_expire_minutes = int(os.getenv("JWT_ACCESS_TOKEN_EXPIRE_MINUTES", "30"))
        self.refresh_token_expire_days = int(os.getenv("JWT_REFRESH_TOKEN_EXPIRE_DAYS", "7"))
        
        self.pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
        self.verification_codes = {}
        self.verification_code_expiry = {}
    
    def create_access_token(self, data: dict, expires_delta: Optional[timedelta] = None) -> Tuple[str, datetime]:
        to_encode = data.copy()
        if expires_delta:
            expire = datetime.utcnow() + expires_delta
        else:
            expire = datetime.utcnow() + timedelta(minutes=self.access_token_expire_minutes)
        
        to_encode.update({"exp": expire})
        encoded_jwt = jwt.encode(to_encode, self.secret_key, algorithm=self.algorithm)
        return encoded_jwt, expire
    
    def create_refresh_token(self, data: dict, expires_delta: Optional[timedelta] = None) -> Tuple[str, datetime]:
        to_encode = data.copy()
        if expires_delta:
            expire = datetime.utcnow() + expires_delta
        else:
            expire = datetime.utcnow() + timedelta(days=self.refresh_token_expire_days)
        
        to_encode.update({"exp": expire})
        encoded_jwt = jwt.encode(to_encode, self.secret_key, algorithm=self.algorithm)
        return encoded_jwt, expire
    
    def verify_token(self, token: str) -> Optional[dict]:
        try:
            payload = jwt.decode(token, self.secret_key, algorithms=[self.algorithm])
            return payload
        except JWTError:
            return None
    
    def generate_verification_code(self, length: int = 6) -> str:
        return ''.join(random.choices(string.digits, k=length))
    
    def save_verification_code(self, phone_number: str, code: str, expires_in: int = 300):
        self.verification_codes[phone_number] = code
        expiry = datetime.utcnow() + timedelta(seconds=expires_in)
        self.verification_code_expiry[phone_number] = expiry
    
    def verify_verification_code(self, phone_number: str, code: str) -> bool:
        if phone_number not in self.verification_codes:
            return False
        
        if phone_number in self.verification_code_expiry:
            if datetime.utcnow() > self.verification_code_expiry[phone_number]:
                del self.verification_codes[phone_number]
                del self.verification_code_expiry[phone_number]
                return False
        
        if self.verification_codes[phone_number] == code:
            del self.verification_codes[phone_number]
            if phone_number in self.verification_code_expiry:
                del self.verification_code_expiry[phone_number]
            return True
        
        return False
    
    def generate_uid(self) -> str:
        return ''.join(random.choices(string.digits, k=8))
    
    def hash_password(self, password: str) -> str:
        return self.pwd_context.hash(password)
    
    def verify_password(self, plain_password: str, hashed_password: str) -> bool:
        return self.pwd_context.verify(plain_password, hashed_password)
