import * as path from 'path';
import * as vscode from 'vscode';
import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
    TransportKind
} from 'vscode-languageclient/node';

let client: LanguageClient;

export function activate(context: vscode.ExtensionContext) {
    console.log('AvanueUI Language Server extension activated');

    // Get JAR path from configuration
    const config = vscode.workspace.getConfiguration('avanueui');
    const jarPath = config.get<string>('server.jarPath');

    if (!jarPath) {
        vscode.window.showErrorMessage(
            'AvanueUI Language Server JAR path not configured. Please set avanueui.server.jarPath in settings.'
        );
        return;
    }

    // Server options: launch JAR with java
    const serverOptions: ServerOptions = {
        command: 'java',
        args: ['-jar', jarPath],
        transport: TransportKind.stdio
    };

    // Client options: define file patterns for AvanueUI files (new + legacy)
    const clientOptions: LanguageClientOptions = {
        documentSelector: [
            { scheme: 'file', language: 'avanueui-yaml' },
            { scheme: 'file', language: 'avanueui-json' },
            { scheme: 'file', pattern: '**/*.avanueui.yaml' },
            { scheme: 'file', pattern: '**/*.avanueui.json' },
            { scheme: 'file', pattern: '**/*.avanueui' },
            { scheme: 'file', pattern: '**/*.magic.yaml' },
            { scheme: 'file', pattern: '**/*.magic.json' },
            { scheme: 'file', pattern: '**/*.magicui' }
        ],
        synchronize: {
            fileEvents: vscode.workspace.createFileSystemWatcher('**/*.{avanueui,magic}.{yaml,json}')
        }
    };

    // Create and start the language client
    client = new LanguageClient(
        'avanueui-lsp',
        'AvanueUI Language Server',
        serverOptions,
        clientOptions
    );

    // Start the client (this will also launch the server)
    client.start();

    console.log('AvanueUI Language Server client started');
}

export function deactivate(): Thenable<void> | undefined {
    if (!client) {
        return undefined;
    }
    return client.stop();
}
