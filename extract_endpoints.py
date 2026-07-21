import os, re

controller_dir = r'd:\projeto_moises\gcoedu_core_java\src\main\java\com\gcoedu\core\controller'
endpoints = []

for root, dirs, files in os.walk(controller_dir):
    for file in files:
        if file.endswith('.java'):
            with open(os.path.join(root, file), 'r', encoding='utf-8') as f:
                content = f.read()
                
                # Find class level RequestMapping
                class_mapping = re.search(r'@RequestMapping\((?:value\s*=\s*)?\{?(?:\"([^\"]+)\"[^}]*)\}?\)', content)
                if not class_mapping:
                    class_mapping = re.search(r'@RequestMapping\(\"([^\"]+)\"\)', content)
                
                base_path = class_mapping.group(1) if class_mapping else ''
                
                # Find method level mappings
                method_mappings = re.findall(r'@(Get|Post|Put|Delete|Patch)Mapping\((?:value\s*=\s*)?\{?(?:\"([^\"]*)\"[^}]*)\}?\)', content)
                # also handle empty mapping like @GetMapping
                empty_mappings = re.findall(r'@(Get|Post|Put|Delete|Patch)Mapping(?!\()', content)
                
                for method, path in method_mappings:
                    full_path = base_path + path
                    # fix double slashes
                    full_path = full_path.replace('//', '/')
                    endpoints.append(f'- **{method.upper()}** `{full_path}` ({file})')
                    
                for method in empty_mappings:
                    endpoints.append(f'- **{method.upper()}** `{base_path}` ({file})')

with open(r'd:\projeto_moises\gcoedu_core_java\endpoints_list.md', 'w', encoding='utf-8') as out:
    out.write('# Lista de Endpoints do Backend\n\n')
    for ep in sorted(endpoints):
        out.write(ep + '\n')
print('Done!')
