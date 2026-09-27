"""Run the two-participant demo on loopback with freshly generated credentials."""
import argparse
import hashlib
import os
import pathlib
import secrets
import shutil
import subprocess
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent.parent

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port', type=int, default=8080)
    args = parser.parse_args()
    if not 1024 <= args.port <= 65535:
        parser.error('Use a port from 1024 to 65535')
    java = os.environ.get('JAVA_EXE') or shutil.which('java')
    if not java and os.environ.get('JAVA_HOME'):
        java = str(pathlib.Path(os.environ['JAVA_HOME'])/'bin'/('java.exe' if os.name=='nt' else 'java'))
    if not java:
        parser.error('Set JAVA_HOME/JAVA_EXE or add Java to PATH')
    libraries = ROOT/'server/build/install/server/lib'
    if not libraries.is_dir():
        parser.error('Build first: ./gradlew :server:installDist (gradlew.bat on Windows)')
    private = ROOT/'.local'
    private.mkdir(exist_ok=True)
    codes = {u: str(100000+secrets.randbelow(900000)) for u in ('a','b')}
    while codes['a'] == codes['b']:
        codes['b'] = str(100000+secrets.randbelow(900000))
    with tempfile.TemporaryDirectory(prefix='demo-', dir=private) as directory:
        secret = pathlib.Path(directory)/'turn-secret'
        secret.write_text(secrets.token_hex(32))
        secret.chmod(0o600)
        env = dict(os.environ, PUBLIC_ORIGIN=f'http://localhost:{args.port}', PORT=str(args.port), TURN_HOST='turn.example.invalid', CREDENTIALS_DIRECTORY=directory)
        for u, code in codes.items():
            env['LOGIN_'+u.upper()+'_SHA256'] = hashlib.sha256(code.encode()).hexdigest()
        print(f'Local-only demo: http://localhost:{args.port}', flush=True)
        print(f'Participant 1: {codes["a"]}\nParticipant 2: {codes["b"]}', flush=True)
        print('Use two browser profiles. These codes expire when this process stops. Ctrl+C stops the demo.', flush=True)
        process = subprocess.Popen([java, '-cp', str(libraries/'*'), 'dev.dentag.darou.server.MainKt'], env=env)
        try:
            return process.wait()
        except KeyboardInterrupt:
            return 0
        finally:
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    process.kill(); process.wait()

if __name__ == '__main__':
    raise SystemExit(main())
