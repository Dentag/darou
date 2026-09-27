"""Small repository guard. Prints filenames/rule names, never matched credentials."""
import argparse
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent

def git(*args):
    return subprocess.check_output(['git', '-c', 'safe.directory='+ROOT.as_posix(), '-C', str(ROOT), *args])

def private_path(name):
    p = pathlib.PurePosixPath(name)
    if any(part in {'.local', 'secrets', 'credentials', '.ssh', '.idea', 'node_modules', 'xcuserdata'} for part in p.parts):
        return True
    if p.name in {
        'local.properties', 'key.properties', 'keystore.properties', 'signing.properties',
        'secrets.properties', 'app-access.json', 'turn-secret', 'google-services.json',
        'GoogleService-Info.plist',
    } or p.name.startswith(('id_rsa', 'id_ed25519')):
        return True
    if p.match('service-account*.json') or p.match('*-firebase-adminsdk-*.json'):
        return True
    if (p.name == '.env' or '.env.' in p.name or p.name.endswith('.env')) and not p.name.endswith('.example'):
        return True
    return p.suffix.lower() in {
        '.key', '.pem', '.p8', '.ppk', '.crt', '.cer', '.der', '.p12', '.pfx', '.jks',
        '.keystore', '.mobileprovision', '.provisionprofile', '.log', '.hprof', '.jfr',
    }

RULES = {
    'private key': re.compile(rb'-----BEGIN (?:[A-Z0-9]+ )*PRIVATE KEY-----'),
    'GitHub credential': re.compile(rb'\b(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{30,})\b'),
    'AWS access key': re.compile(rb'\b(?:AKIA|ASIA)[A-Z0-9]{16}\b'),
    'URL with password': re.compile(rb'https?://[^\s/@:]+:[^\s/@]+@'),
    'configured login hash': re.compile(rb'LOGIN_[AB]_SHA256\s*[:=]\s*[\x22\x27]?[a-fA-F0-9]{64}\b'),
    'hardcoded demo login': re.compile(rb'(?:LOGIN_CODE|ACCESS_CODE|\x22[ab]\x22)\s*[:=]\s*[\x22\x27][0-9]{4,8}[\x22\x27]'),
    'configured TURN secret': re.compile(rb'static-auth-secret\s*=\s*[a-fA-F0-9]{32,}\b'),
}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--staged', action='store_true', help='Read exact index contents for a pre-commit check')
    parser.add_argument('--tracked', action='store_true', help='Only tracked files (CI)')
    args = parser.parse_args()
    options = ['ls-files', '-z', '--cached']
    if not args.staged and not args.tracked:
        options += ['--others', '--exclude-standard']
    names = sorted(set(git(*options).decode('utf-8').rstrip('\0').split('\0')) - {''})
    failures = []
    for name in names:
        if private_path(name):
            failures.append((name, 'private file type/path'))
        if args.staged:
            data = git('show', ':'+name)
        else:
            path = ROOT/name
            if not path.is_file():
                continue
            data = path.read_bytes()
        if name == 'gradle/wrapper/gradle-wrapper.jar':
            continue
        for label, pattern in RULES.items():
            if pattern.search(data):
                failures.append((name, label))
    for name, label in failures:
        print(f'BLOCKED {name}: {label}', file=sys.stderr)
    print(f'Checked {len(names)} repository files; {len(failures)} findings. Review is still required.')
    return bool(failures)

if __name__ == '__main__':
    sys.exit(main())
