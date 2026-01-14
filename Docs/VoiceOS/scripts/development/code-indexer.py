#!/usr/bin/env python3
# filename: code-indexer.py
# created: 2025-08-21 21:55:00 PST
# author: Manoj Jhawar
# © Augmentalis Inc, Intelligent Devices LLC, Manoj Jhawar, Aman Jhawar
# TCR: Pre-implementation Analysis Completed
# agent: DevOps Agent - Expert Level | mode: ACT

"""
VOS4 Code Indexer - Automated Master Index Generator
Advanced code analysis and documentation generation for large-scale Android projects
"""

import os
import re
import json
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Set, Tuple
import argparse

class KotlinCodeAnalyzer:
    """Expert-level Kotlin code analysis for Android projects"""
    
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.modules = {}
        self.classes = {}
        self.functions = {}
        self.interfaces = {}
        self.dependencies = {}
        
    def scan_project(self) -> Dict:
        """Comprehensive project scanning with advanced pattern recognition"""
        print("🔍 Starting comprehensive code analysis...")
        
        # Scan all Kotlin files in the project
        kotlin_files = list(self.project_root.rglob("*.kt"))
        java_files = list(self.project_root.rglob("*.java"))
        
        print(f"📁 Found {len(kotlin_files)} Kotlin files and {len(java_files)} Java files")
        
        for file_path in kotlin_files:
            self._analyze_kotlin_file(file_path)
            
        for file_path in java_files:
            self._analyze_java_file(file_path)
            
        return self._generate_index_data()
    
    def _analyze_kotlin_file(self, file_path: Path):
        """Advanced Kotlin file analysis with comprehensive pattern matching"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                
            # Extract module information from path
            module_name = self._extract_module_name(file_path)
            
            # Extract package declaration
            package_match = re.search(r'package\s+([\w.]+)', content)
            package_name = package_match.group(1) if package_match else ""
            
            # Extract classes and interfaces
            self._extract_classes(content, file_path, package_name, module_name)
            self._extract_interfaces(content, file_path, package_name, module_name)
            self._extract_functions(content, file_path, package_name, module_name)
            self._extract_dependencies(content, file_path, module_name)
            
        except Exception as e:
            print(f"⚠️  Error analyzing {file_path}: {e}")
    
    def _analyze_java_file(self, file_path: Path):
        """Java file analysis for legacy components"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                
            module_name = self._extract_module_name(file_path)
            
            package_match = re.search(r'package\s+([\w.]+);', content)
            package_name = package_match.group(1) if package_match else ""
            
            # Extract Java classes
            class_pattern = r'(?:public\s+)?(?:abstract\s+)?(?:final\s+)?class\s+(\w+)(?:\s+extends\s+\w+)?(?:\s+implements\s+[\w\s,]+)?'
            for match in re.finditer(class_pattern, content):
                class_name = match.group(1)
                self.classes[f"{package_name}.{class_name}"] = {
                    'name': class_name,
                    'package': package_name,
                    'module': module_name,
                    'file': str(file_path.relative_to(self.project_root)),
                    'type': 'class',
                    'language': 'java'
                }
                
        except Exception as e:
            print(f"⚠️  Error analyzing {file_path}: {e}")
    
    def _extract_module_name(self, file_path: Path) -> str:
        """Extract module name from file path with VOS4 structure awareness"""
        parts = file_path.relative_to(self.project_root).parts
        
        # VOS4-specific module detection
        if 'apps' in parts:
            idx = parts.index('apps')
            return parts[idx + 1] if idx + 1 < len(parts) else 'unknown'
        elif 'managers' in parts:
            idx = parts.index('managers')
            return parts[idx + 1] if idx + 1 < len(parts) else 'unknown'
        elif 'libraries' in parts:
            idx = parts.index('libraries')
            return parts[idx + 1] if idx + 1 < len(parts) else 'unknown'
        else:
            return parts[0] if parts else 'root'
    
    def _extract_classes(self, content: str, file_path: Path, package: str, module: str):
        """Advanced class extraction with comprehensive pattern matching"""
        # Class patterns: data class, sealed class, abstract class, regular class, object
        patterns = [
            r'(?:data\s+)?(?:sealed\s+)?(?:abstract\s+)?(?:open\s+)?class\s+(\w+)(?:\s*<[^>]*>)?(?:\s*:\s*[^{]+)?',
            r'object\s+(\w+)(?:\s*:\s*[^{]+)?',
            r'enum\s+class\s+(\w+)',
            r'annotation\s+class\s+(\w+)'
        ]
        
        for pattern in patterns:
            for match in re.finditer(pattern, content):
                class_name = match.group(1)
                full_name = f"{package}.{class_name}" if package else class_name
                
                # Extract methods for this class
                methods = self._extract_class_methods(content, class_name)
                
                self.classes[full_name] = {
                    'name': class_name,
                    'package': package,
                    'module': module,
                    'file': str(file_path.relative_to(self.project_root)),
                    'methods': methods,
                    'type': self._determine_class_type(match.group(0)),
                    'language': 'kotlin'
                }
    
    def _extract_interfaces(self, content: str, file_path: Path, package: str, module: str):
        """Extract interface definitions with method signatures"""
        pattern = r'interface\s+(\w+)(?:\s*<[^>]*>)?(?:\s*:\s*[^{]+)?'
        
        for match in re.finditer(pattern, content):
            interface_name = match.group(1)
            full_name = f"{package}.{interface_name}" if package else interface_name
            
            # Extract interface methods
            methods = self._extract_interface_methods(content, interface_name)
            
            self.interfaces[full_name] = {
                'name': interface_name,
                'package': package,
                'module': module,
                'file': str(file_path.relative_to(self.project_root)),
                'methods': methods,
                'language': 'kotlin'
            }
    
    def _extract_functions(self, content: str, file_path: Path, package: str, module: str):
        """Extract top-level and extension functions"""
        # Top-level functions
        pattern = r'(?:inline\s+)?(?:suspend\s+)?fun\s+(?:<[^>]*>\s+)?(\w+)\s*\([^)]*\)(?:\s*:\s*[^{=]+)?'
        
        for match in re.finditer(pattern, content):
            func_name = match.group(1)
            
            # Skip functions inside classes (basic heuristic)
            func_start = match.start()
            preceding_text = content[:func_start].split('\n')[-20:]  # Check last 20 lines
            
            # Skip if we're inside a class or interface
            if any('class ' in line or 'interface ' in line for line in preceding_text):
                continue
                
            full_name = f"{package}.{func_name}" if package else func_name
            
            self.functions[full_name] = {
                'name': func_name,
                'package': package,
                'module': module,
                'file': str(file_path.relative_to(self.project_root)),
                'signature': match.group(0),
                'language': 'kotlin'
            }
    
    def _extract_dependencies(self, content: str, file_path: Path, module: str):
        """Extract import dependencies and module relationships"""
        import_pattern = r'import\s+([\w.]+)(?:\.\*)?'
        
        if module not in self.dependencies:
            self.dependencies[module] = set()
            
        for match in re.finditer(import_pattern, content):
            import_path = match.group(1)
            
            # Extract potential module dependency
            if import_path.startswith('com.ai.'):
                parts = import_path.split('.')
                if len(parts) >= 3:
                    dep_module = parts[2]  # com.ai.[module]
                    if dep_module != module.lower():
                        self.dependencies[module].add(dep_module)
    
    def _extract_class_methods(self, content: str, class_name: str) -> List[str]:
        """Extract methods within a specific class"""
        methods = []
        
        # Find class definition and extract its content
        class_pattern = rf'(?:class|object)\s+{class_name}[^{{]*\{{'
        match = re.search(class_pattern, content)
        
        if match:
            # Find the class block (simplified approach)
            start_pos = match.end()
            # Extract methods within reasonable vicinity
            class_section = content[start_pos:start_pos + 5000]  # Limited scope
            
            method_pattern = r'(?:override\s+)?(?:suspend\s+)?fun\s+(\w+)\s*\([^)]*\)'
            for method_match in re.finditer(method_pattern, class_section):
                methods.append(method_match.group(1))
                
        return methods[:10]  # Limit to first 10 methods
    
    def _extract_interface_methods(self, content: str, interface_name: str) -> List[str]:
        """Extract methods within a specific interface"""
        methods = []
        
        interface_pattern = rf'interface\s+{interface_name}[^{{]*\{{'
        match = re.search(interface_pattern, content)
        
        if match:
            start_pos = match.end()
            interface_section = content[start_pos:start_pos + 3000]
            
            method_pattern = r'(?:suspend\s+)?fun\s+(\w+)\s*\([^)]*\)'
            for method_match in re.finditer(method_pattern, interface_section):
                methods.append(method_match.group(1))
                
        return methods[:10]  # Limit to first 10 methods
    
    def _determine_class_type(self, class_declaration: str) -> str:
        """Determine the type of class from its declaration"""
        if 'data class' in class_declaration:
            return 'data_class'
        elif 'sealed class' in class_declaration:
            return 'sealed_class'
        elif 'abstract class' in class_declaration:
            return 'abstract_class'
        elif 'enum class' in class_declaration:
            return 'enum_class'
        elif 'annotation class' in class_declaration:
            return 'annotation_class'
        elif 'object' in class_declaration:
            return 'object'
        else:
            return 'class'
    
    def _generate_index_data(self) -> Dict:
        """Generate comprehensive index data structure"""
        return {
            'metadata': {
                'generated_at': datetime.now().isoformat(),
                'project_root': str(self.project_root),
                'total_classes': len(self.classes),
                'total_interfaces': len(self.interfaces),
                'total_functions': len(self.functions),
                'modules': list(set(item.get('module', '') for item in {**self.classes, **self.interfaces}.values()))
            },
            'modules': self._organize_by_modules(),
            'classes': self.classes,
            'interfaces': self.interfaces,
            'functions': self.functions,
            'dependencies': {k: list(v) for k, v in self.dependencies.items()}
        }
    
    def _organize_by_modules(self) -> Dict:
        """Organize all components by module"""
        modules = {}
        
        all_components = {**self.classes, **self.interfaces}
        
        for full_name, component in all_components.items():
            module = component.get('module', 'unknown')
            
            if module not in modules:
                modules[module] = {
                    'classes': [],
                    'interfaces': [],
                    'functions': [],
                    'packages': set()
                }
            
            if component.get('type') == 'interface' or full_name in self.interfaces:
                modules[module]['interfaces'].append(component)
            else:
                modules[module]['classes'].append(component)
                
            modules[module]['packages'].add(component.get('package', ''))
        
        # Add functions to modules
        for full_name, function in self.functions.items():
            module = function.get('module', 'unknown')
            if module in modules:
                modules[module]['functions'].append(function)
        
        # Convert sets to lists for JSON serialization
        for module in modules.values():
            module['packages'] = list(module['packages'])
            
        return modules

class IndexGenerator:
    """Generate updated master index file"""
    
    def __init__(self, project_root: str, output_file: str):
        self.project_root = project_root
        self.output_file = output_file
        self.analyzer = KotlinCodeAnalyzer(project_root)
        
    def generate_updated_index(self):
        """Generate updated master code index"""
        print("🚀 Generating updated master code index...")
        
        # Scan project
        index_data = self.analyzer.scan_project()
        
        # Generate markdown content
        markdown_content = self._generate_markdown(index_data)
        
        # Write to file
        with open(self.output_file, 'w', encoding='utf-8') as f:
            f.write(markdown_content)
            
        print(f"✅ Master code index updated: {self.output_file}")
        
        # Generate JSON index for machine processing
        json_file = self.output_file.replace('.md', '.json')
        with open(json_file, 'w', encoding='utf-8') as f:
            json.dump(index_data, f, indent=2)
            
        print(f"📊 JSON index generated: {json_file}")
    
    def _generate_markdown(self, index_data: Dict) -> str:
        """Generate markdown content from index data"""
        metadata = index_data['metadata']
        modules = index_data['modules']
        
        content = f"""<!--
filename: MASTER_CODE_INDEX.md
created: 2025-08-21 21:54:00 PST
author: Manoj Jhawar (Auto-Generated)
© Augmentalis Inc, Intelligent Devices LLC, Manoj Jhawar, Aman Jhawar
TCR: Post-validation Review Completed
agent: DevOps Agent - Expert Level | mode: ACT
-->

# VOS4 Master Code Index (Auto-Generated)
**Last Updated:** {metadata['generated_at']}

## 📊 Project Statistics
- **Total Classes:** {metadata['total_classes']}
- **Total Interfaces:** {metadata['total_interfaces']}
- **Total Functions:** {metadata['total_functions']}
- **Active Modules:** {len(metadata['modules'])}
- **Last Scan:** {metadata['generated_at']}

## 🏗️ Module Overview

"""
        
        for module_name, module_data in modules.items():
            if not module_data['classes'] and not module_data['interfaces']:
                continue
                
            content += f"### **{module_name}**\n"
            content += f"- **Classes:** {len(module_data['classes'])}\n"
            content += f"- **Interfaces:** {len(module_data['interfaces'])}\n"
            content += f"- **Functions:** {len(module_data['functions'])}\n"
            content += f"- **Packages:** {', '.join(sorted(module_data['packages']))}\n\n"
            
            # List key classes
            if module_data['classes']:
                content += "**Key Classes:**\n"
                for cls in sorted(module_data['classes'][:5], key=lambda x: x['name']):
                    methods_str = ', '.join(cls['methods'][:3]) if cls.get('methods') else 'No methods detected'
                    content += f"- `{cls['name']}` - {methods_str}\n"
                content += "\n"
            
            # List interfaces
            if module_data['interfaces']:
                content += "**Interfaces:**\n"
                for iface in sorted(module_data['interfaces'][:3], key=lambda x: x['name']):
                    methods_str = ', '.join(iface['methods'][:3]) if iface.get('methods') else 'No methods detected'
                    content += f"- `{iface['name']}` - {methods_str}\n"
                content += "\n"
        
        content += """
## 🔍 Quick Search Commands

### Find Classes:
```bash
grep -r "class.*ClassName" --include="*.kt" src/
```

### Find Interfaces:
```bash
grep -r "interface.*InterfaceName" --include="*.kt" src/
```

### Find Functions:
```bash
grep -r "fun.*functionName" --include="*.kt" src/
```

## 🔄 Auto-Update Information
- **Update Frequency:** On every build
- **Source:** Automated code scanning
- **Manual Review:** Weekly architecture review
- **Validation:** CI/CD pipeline integration

---
**Generated by VOS4 Code Indexer v1.0**
**Automation Level:** Fully Automated with Manual Review Checkpoints
"""
        
        return content

def main():
    """Main execution function"""
    parser = argparse.ArgumentParser(description='VOS4 Code Indexer')
    parser.add_argument('--project-root', default='.', help='Project root directory')
    parser.add_argument('--output', default='docs/MASTER_CODE_INDEX.md', help='Output file path')
    parser.add_argument('--json', action='store_true', help='Also generate JSON index')
    
    args = parser.parse_args()
    
    generator = IndexGenerator(args.project_root, args.output)
    generator.generate_updated_index()
    
    print("🎉 Code indexing complete!")

if __name__ == "__main__":
    main()
