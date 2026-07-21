import os, re, glob
path = 'D:/projeto_moises/gcoedu_core_java/src/main/java/com/gcoedu/core/domain/entity/tenant/*.java'
for f in glob.glob(path):
    with open(f, 'r', encoding='utf-8') as file:
        content = file.read()
    new_content = re.sub(r'@Table\(\s*name\s*=\s*"([^"]+)"\s*\)', r'@Table(name = "\1", schema = "tenant")', content)
    # Special case for TestQuestion
    new_content = re.sub(r'@Table\(\s*name\s*=\s*"([^"]+)",\s*uniqueConstraints', r'@Table(name = "\1", schema = "tenant", uniqueConstraints', new_content)
    if new_content != content:
        with open(f, 'w', encoding='utf-8') as file:
            file.write(new_content)
