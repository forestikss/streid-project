import os
import time
import subprocess

def get_git_user():
    """Получить имя пользователя из Git"""
    try:
        result = subprocess.run(['git', 'config', 'user.name'], 
                              capture_output=True, text=True)
        return result.stdout.strip() or "Unknown"
    except:
        return "Unknown"

def get_git_status(watch_path):
    """Получить список измененных файлов"""
    try:
        result = subprocess.run(['git', 'status', '--porcelain'], 
                              cwd=watch_path,
                              capture_output=True, text=True)
        return result.stdout.strip()
    except:
        return ""

def sync(watch_path, username):
    try:
        print(f"\n{'─'*60}")
        print(f"[{time.strftime('%H:%M:%S')}] 🔄 Цикл синхронизации")
        print(f"{'─'*60}")
        
        # Pull изменения от друга
        print(f"[PULL] Запрос изменений с GitHub...")
        pull_result = subprocess.run(['git', 'pull', '--rebase'], 
                     cwd=watch_path, 
                     capture_output=True, text=True)
        
        print(f"[PULL] Код возврата: {pull_result.returncode}")
        if pull_result.stdout:
            print(f"[PULL] Вывод: {pull_result.stdout.strip()}")
        if pull_result.stderr:
            print(f"[PULL] Ошибки: {pull_result.stderr.strip()}")
        
        # Проверяем кто делал изменения на GitHub
        if 'Already up to date' not in pull_result.stdout:
            log_result = subprocess.run(['git', 'log', '-1', '--pretty=format:%an изменил файлы'], 
                                      cwd=watch_path,
                                      capture_output=True, text=True)
            if log_result.stdout:
                print(f"[PULL] ✓ {log_result.stdout}")
        
        # Проверяем локальные изменения
        print(f"\n[STATUS] Проверка локальных изменений...")
        status_result = subprocess.run(['git', 'status', '--porcelain'], 
                              cwd=watch_path,
                              capture_output=True, text=True)
        status = status_result.stdout.strip()
        
        print(f"[STATUS] Код возврата: {status_result.returncode}")
        if status:
            print(f"[STATUS] Найдены изменения:")
            print(status)
        else:
            print(f"[STATUS] Изменений нет")
            return
        
        print(f"\n[CHANGES] {username} обнаружил изменения:")
        
        # Показываем что изменилось
        for line in status.split('\n'):
            if line.strip():
                status_code = line[:2]
                filename = line[3:].strip()
                
                if 'M' in status_code:
                    print(f"  📝 ИЗМЕНЕН: {filename}")
                elif 'A' in status_code or '?' in status_code:
                    print(f"  ➕ СОЗДАН: {filename}")
                elif 'D' in status_code:
                    print(f"  ❌ УДАЛЕН: {filename}")
                else:
                    print(f"  ❓ {status_code}: {filename}")
        
        # Добавить все изменения
        print(f"\n[ADD] Добавление файлов в индекс...")
        add_result = subprocess.run(['git', 'add', '-A'], 
                     cwd=watch_path,
                     capture_output=True, text=True)
        print(f"[ADD] Код возврата: {add_result.returncode}")
        if add_result.stderr:
            print(f"[ADD] Ошибки: {add_result.stderr.strip()}")
        
        # Commit
        print(f"\n[COMMIT] Создание коммита...")
        commit_result = subprocess.run(['git', 'commit', '-m', f'Auto sync by {username}'], 
                              cwd=watch_path,
                              capture_output=True, text=True)
        print(f"[COMMIT] Код возврата: {commit_result.returncode}")
        if commit_result.stdout:
            print(f"[COMMIT] Вывод: {commit_result.stdout.strip()}")
        if commit_result.stderr:
            print(f"[COMMIT] Ошибки: {commit_result.stderr.strip()}")
        
        # Push если есть изменения
        if commit_result.returncode == 0:
            print(f"\n[PUSH] Отправка на GitHub...")
            push_result = subprocess.run(['git', 'push'], 
                         cwd=watch_path,
                         capture_output=True, text=True)
            print(f"[PUSH] Код возврата: {push_result.returncode}")
            if push_result.stdout:
                print(f"[PUSH] Вывод: {push_result.stdout.strip()}")
            if push_result.stderr:
                print(f"[PUSH] Ошибки: {push_result.stderr.strip()}")
            
            if push_result.returncode == 0:
                print(f"\n✅ {username} УСПЕШНО синхронизировал изменения!")
            else:
                print(f"\n⚠️ Ошибка при отправке на GitHub")
        else:
            print(f"\n⚠️ Коммит не создан (возможно нет изменений)")
            
    except Exception as e:
        print(f"\n❌ ИСКЛЮЧЕНИЕ: {e}")
        import traceback
        traceback.print_exc()

def main():
    watch_path = input("Путь к папке проекта (или . для текущей): ").strip() or "."
    watch_path = os.path.abspath(watch_path)
    
    if not os.path.exists(os.path.join(watch_path, '.git')):
        print("Это не git репозиторий! Инициализируйте git сначала.")
        return
    
    username = get_git_user()
    
    print(f"\n{'='*50}")
    print(f"Автосинхронизация запущена")
    print(f"Пользователь: {username}")
    print(f"Папка: {watch_path}")
    print(f"{'='*50}")
    print("Проверка каждые 3 секунды...")
    print("Нажмите Ctrl+C для остановки\n")
    
    try:
        while True:
            sync(watch_path, username)
            time.sleep(3)
    except KeyboardInterrupt:
        print("\n\nОстановлено")

if __name__ == "__main__":
    main()
