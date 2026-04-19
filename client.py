import asyncio
import websockets
import json
import sys
from pathlib import Path

class CollaborativeClient:
    def __init__(self, server_url, file_path):
        self.server_url = server_url
        self.file_path = Path(file_path)
        self.websocket = None
        self.last_content = ""
    
    async def connect(self):
        """Подключение к серверу"""
        self.websocket = await websockets.connect(self.server_url)
        print(f"Подключено к {self.server_url}")
        
        # Получаем начальный код
        message = await self.websocket.recv()
        data = json.loads(message)
        if data["type"] == "init":
            self.last_content = data["code"]
            self.file_path.write_text(self.last_content, encoding="utf-8")
            print(f"Файл {self.file_path} синхронизирован")
    
    async def watch_file(self):
        """Отслеживание изменений в файле"""
        while True:
            await asyncio.sleep(1)
            
            if self.file_path.exists():
                current_content = self.file_path.read_text(encoding="utf-8")
                
                if current_content != self.last_content:
                    self.last_content = current_content
                    await self.websocket.send(json.dumps({
                        "type": "update",
                        "code": current_content
                    }))
                    print("Изменения отправлены на сервер")
    
    async def receive_updates(self):
        """Получение обновлений от сервера"""
        async for message in self.websocket:
            data = json.loads(message)
            
            if data["type"] == "update":
                self.last_content = data["code"]
                self.file_path.write_text(self.last_content, encoding="utf-8")
                print("Получены изменения от другого пользователя")
    
    async def run(self):
        """Запуск клиента"""
        await self.connect()
        
        # Запускаем две задачи параллельно
        await asyncio.gather(
            self.watch_file(),
            self.receive_updates()
        )

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Использование: python client.py <server_url> <file_path>")
        print("Пример: python client.py ws://localhost:8765 shared_code.py")
        sys.exit(1)
    
    server_url = sys.argv[1]
    file_path = sys.argv[2]
    
    client = CollaborativeClient(server_url, file_path)
    asyncio.run(client.run())
