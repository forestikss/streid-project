import os
import time
import subprocess
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler

class GitAutoSync(FileSystemEventHandler):
    def __init__(self, watch_path, interval=3):
        self.watch_path = watch_path
        self.interval = interval
        self.last_sync = 0
        
    def on_modified(self, event):
        if event.is_directory or '.git' in event.src_path:
            return
        
        current_time = time.time()
        if current_time - self.last_sync < self.interval:
            return
            
        self.last_sync = current_time
        self.sync()
    
    def sync(self):
        try:
            # Pull изменения от друга
            subprocess.run(['git', 'pull', '--rebase'], 
                         cwd=self.watch_path, 
                         capture_output=True)
            
            # Добавить все изменения
            subprocess.run(['git', 'add', '.'], 
                         cwd=self.watch_path,
                         capture_output=True)
            
            # Commit
            result = subprocess.run(['git', 'commit', '-m', 'Auto sync'], 
                                  cwd=self.watch_path,
                                  capture_output=True)
            
            # Push если есть изменения
            if result.returncode == 0:
                subprocess.run(['git', 'push'], 
                             cwd=self.watch_path,
                             capture_output=True)
                print(f"✓ Синхронизировано: {time.strftime('%H:%M:%S')}")
                
        except Exception as e:
            print(f"Ошибка синхронизации: {e}")

def main():
    watch_path = input("Путь к папке проекта (или . для текущей): ").strip() or "."
    watch_path = os.path.abspath(watch_path)
    
    if not os.path.exists(os.path.join(watch_path, '.git')):
        print("Это не git репозиторий! Инициализируйте git сначала.")
        return
    
    print(f"Автосинхронизация запущена для: {watch_path}")
    print("Нажмите Ctrl+C для остановки\n")
    
    event_handler = GitAutoSync(watch_path)
    observer = Observer()
    observer.schedule(event_handler, watch_path, recursive=True)
    observer.start()
    
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()
        print("\nОстановлено")
    
    observer.join()

if __name__ == "__main__":
    main()
