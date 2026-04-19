import asyncio
import websockets
import json

# Хранилище подключенных клиентов и общего кода
clients = set()
shared_code = ""

async def handle_client(websocket):
    """Обработка подключения клиента"""
    clients.add(websocket)
    print(f"Новый клиент подключен. Всего клиентов: {len(clients)}")
    
    try:
        # Отправляем текущий код новому клиенту
        await websocket.send(json.dumps({
            "type": "init",
            "code": shared_code
        }))
        
        async for message in websocket:
            data = json.loads(message)
            
            if data["type"] == "update":
                global shared_code
                shared_code = data["code"]
                
                # Отправляем обновление всем клиентам кроме отправителя
                for client in clients:
                    if client != websocket:
                        await client.send(json.dumps({
                            "type": "update",
                            "code": shared_code
                        }))
    
    except websockets.exceptions.ConnectionClosed:
        pass
    finally:
        clients.remove(websocket)
        print(f"Клиент отключен. Осталось клиентов: {len(clients)}")

async def main():
    """Запуск сервера"""
    print("Сервер запущен на ws://localhost:8765")
    print("Ожидание подключений...")
    async with websockets.serve(handle_client, "0.0.0.0", 8765):
        await asyncio.Future()

if __name__ == "__main__":
    asyncio.run(main())
