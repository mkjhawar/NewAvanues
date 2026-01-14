# Chapter 14: P2P/WebRTC Collaboration

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net
**Word Count:** ~3,500 words

---

## Overview

Peer-to-peer collaboration enables real-time screen sharing, collaborative editing, and voice communication using WebRTC.

**Status:** ❌ Not implemented
**Effort:** 160 hours (Weeks 9-12)

## WebRTC Architecture

```
Client A                 Signaling Server          Client B
   │                            │                      │
   ├─ createOffer() ────────────>│                      │
   │                            ├─ forward offer ─────>│
   │                            │                      ├─ createAnswer()
   │                            │<─ forward answer ────┤
   │<─────── ICE candidates ────┼────── ICE candidates ────>│
   │                            │                      │
   └──────────── Direct P2P Data Channel ──────────────┘
```

## WebRTC Client Implementation

```typescript
// WebRTCClient.ts

export class WebRTCClient {
  private socket: SocketIOClient.Socket;
  private peerConnections: Map<string, RTCPeerConnection> = new Map();
  private dataChannels: Map<string, RTCDataChannel> = new Map();

  private iceServers: RTCIceServer[] = [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
    {
      urls: 'turn:turn.example.com:3478',
      username: 'user',
      credential: 'password'
    }
  ];

  constructor(signalingServerUrl: string) {
    this.socket = io(signalingServerUrl);
    this.setupSocketHandlers();
  }

  async connect(roomId: string, userId: string): Promise<void> {
    this.socket.emit('join', { roomId, userId });
  }

  private setupSocketHandlers(): void {
    this.socket.on('user-joined', async (data: { userId: string }) => {
      await this.createPeerConnection(data.userId, true);
    });

    this.socket.on('offer', async (data: { from: string; offer: RTCSessionDescriptionInit }) => {
      await this.handleOffer(data.from, data.offer);
    });

    this.socket.on('answer', async (data: { from: string; answer: RTCSessionDescriptionInit }) => {
      await this.handleAnswer(data.from, data.answer);
    });

    this.socket.on('ice-candidate', async (data: { from: string; candidate: RTCIceCandidateInit }) => {
      await this.handleIceCandidate(data.from, data.candidate);
    });
  }

  private async createPeerConnection(userId: string, isInitiator: boolean): Promise<void> {
    const pc = new RTCPeerConnection({ iceServers: this.iceServers });

    // ICE candidate handler
    pc.onicecandidate = (event) => {
      if (event.candidate) {
        this.socket.emit('ice-candidate', {
          to: userId,
          candidate: event.candidate
        });
      }
    };

    // Connection state handler
    pc.onconnectionstatechange = () => {
      console.log(`Connection state: ${pc.connectionState}`);
      if (pc.connectionState === 'failed') {
        this.reconnect(userId);
      }
    };

    // Data channel handler
    if (isInitiator) {
      const dataChannel = pc.createDataChannel('collaboration');
      this.setupDataChannel(userId, dataChannel);
    } else {
      pc.ondatachannel = (event) => {
        this.setupDataChannel(userId, event.channel);
      };
    }

    this.peerConnections.set(userId, pc);

    if (isInitiator) {
      const offer = await pc.createOffer();
      await pc.setLocalDescription(offer);
      this.socket.emit('offer', { to: userId, offer });
    }
  }

  private setupDataChannel(userId: string, channel: RTCDataChannel): void {
    channel.onopen = () => {
      console.log(`Data channel open with ${userId}`);
    };

    channel.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.handleDataChannelMessage(userId, message);
    };

    this.dataChannels.set(userId, channel);
  }

  sendToAll(message: any): void {
    const data = JSON.stringify(message);
    this.dataChannels.forEach((channel) => {
      if (channel.readyState === 'open') {
        channel.send(data);
      }
    });
  }

  sendToPeer(userId: string, message: any): void {
    const channel = this.dataChannels.get(userId);
    if (channel && channel.readyState === 'open') {
      channel.send(JSON.stringify(message));
    }
  }
}
```

## Signaling Server (Node.js + Socket.io)

```typescript
// signaling-server.ts

import express from 'express';
import http from 'http';
import { Server } from 'socket.io';

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: { origin: '*' }
});

interface Room {
  id: string;
  users: Set<string>;
}

const rooms = new Map<string, Room>();

io.on('connection', (socket) => {
  console.log(`Client connected: ${socket.id}`);

  socket.on('join', ({ roomId, userId }) => {
    socket.join(roomId);

    let room = rooms.get(roomId);
    if (!room) {
      room = { id: roomId, users: new Set() };
      rooms.set(roomId, room);
    }

    // Notify existing users
    room.users.forEach((existingUserId) => {
      socket.to(roomId).emit('user-joined', { userId });
    });

    room.users.add(userId);
    console.log(`User ${userId} joined room ${roomId}`);
  });

  socket.on('offer', ({ to, offer }) => {
    io.to(to).emit('offer', { from: socket.id, offer });
  });

  socket.on('answer', ({ to, answer }) => {
    io.to(to).emit('answer', { from: socket.id, answer });
  });

  socket.on('ice-candidate', ({ to, candidate }) => {
    io.to(to).emit('ice-candidate', { from: socket.id, candidate });
  });

  socket.on('disconnect', () => {
    rooms.forEach((room) => {
      if (room.users.has(socket.id)) {
        room.users.delete(socket.id);
        socket.to(room.id).emit('user-left', { userId: socket.id });
      }
    });
  });
});

server.listen(8080, () => {
  console.log('Signaling server running on port 8080');
});
```

## TURN/STUN Server Setup (Coturn)

```bash
# Install coturn
sudo apt-get install coturn

# /etc/turnserver.conf
listening-port=3478
fingerprint
lt-cred-mech
user=username:password
realm=avanues.com
external-ip=<your-public-ip>

# Start server
sudo systemctl start coturn
```

## Collaborative Editing

```typescript
// CollaborativeEditor.ts

interface EditOperation {
  type: 'insert' | 'delete' | 'update';
  componentId: string;
  property?: string;
  value?: any;
  timestamp: number;
  userId: string;
}

export class CollaborativeEditor {
  private webrtc: WebRTCClient;
  private components: ComponentNode[] = [];
  private pendingOperations: EditOperation[] = [];

  constructor(webrtc: WebRTCClient) {
    this.webrtc = webrtc;
    this.setupCollaboration();
  }

  private setupCollaboration(): void {
    this.webrtc.on('edit-operation', (operation: EditOperation) => {
      this.applyOperation(operation);
    });
  }

  addComponent(component: ComponentNode): void {
    this.components.push(component);

    const operation: EditOperation = {
      type: 'insert',
      componentId: component.id,
      value: component,
      timestamp: Date.now(),
      userId: this.webrtc.userId
    };

    this.webrtc.sendToAll({
      type: 'edit-operation',
      operation
    });
  }

  updateComponent(id: string, property: string, value: any): void {
    const component = this.components.find(c => c.id === id);
    if (component) {
      component.properties[property] = value;

      const operation: EditOperation = {
        type: 'update',
        componentId: id,
        property,
        value,
        timestamp: Date.now(),
        userId: this.webrtc.userId
      };

      this.webrtc.sendToAll({
        type: 'edit-operation',
        operation
      });
    }
  }

  private applyOperation(operation: EditOperation): void {
    switch (operation.type) {
      case 'insert':
        this.components.push(operation.value);
        break;
      case 'update':
        const component = this.components.find(c => c.id === operation.componentId);
        if (component && operation.property) {
          component.properties[operation.property] = operation.value;
        }
        break;
      case 'delete':
        this.components = this.components.filter(c => c.id !== operation.componentId);
        break;
    }

    // Trigger UI update
    this.onComponentsChanged(this.components);
  }

  private onComponentsChanged(components: ComponentNode[]): void {
    // Update UI
  }
}
```

## Screen Sharing

```typescript
// ScreenShare.ts

export class ScreenShareManager {
  private webrtc: WebRTCClient;
  private localStream: MediaStream | null = null;

  async startScreenShare(): Promise<void> {
    try {
      this.localStream = await navigator.mediaDevices.getDisplayMedia({
        video: {
          cursor: 'always'
        },
        audio: false
      });

      // Add tracks to all peer connections
      this.webrtc.peerConnections.forEach((pc) => {
        this.localStream!.getTracks().forEach((track) => {
          pc.addTrack(track, this.localStream!);
        });
      });

      // Notify peers
      this.webrtc.sendToAll({
        type: 'screen-share-started'
      });
    } catch (error) {
      console.error('Screen share error:', error);
    }
  }

  stopScreenShare(): void {
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => track.stop());
      this.localStream = null;

      this.webrtc.sendToAll({
        type: 'screen-share-stopped'
      });
    }
  }
}
```

## Voice Chat

```typescript
// VoiceChat.ts

export class VoiceChatManager {
  private webrtc: WebRTCClient;
  private localAudioStream: MediaStream | null = null;

  async startVoiceChat(): Promise<void> {
    try {
      this.localAudioStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        },
        video: false
      });

      // Add audio tracks to all peer connections
      this.webrtc.peerConnections.forEach((pc) => {
        this.localAudioStream!.getTracks().forEach((track) => {
          pc.addTrack(track, this.localAudioStream!);
        });
      });
    } catch (error) {
      console.error('Voice chat error:', error);
    }
  }

  stopVoiceChat(): void {
    if (this.localAudioStream) {
      this.localAudioStream.getTracks().forEach(track => track.stop());
      this.localAudioStream = null;
    }
  }

  mute(): void {
    if (this.localAudioStream) {
      this.localAudioStream.getAudioTracks().forEach(track => {
        track.enabled = false;
      });
    }
  }

  unmute(): void {
    if (this.localAudioStream) {
      this.localAudioStream.getAudioTracks().forEach(track => {
        track.enabled = true;
      });
    }
  }
}
```

## Presence & Cursors

```typescript
// PresenceManager.ts

interface UserPresence {
  userId: string;
  username: string;
  color: string;
  cursor: { x: number; y: number } | null;
  isActive: boolean;
  lastSeen: number;
}

export class PresenceManager {
  private webrtc: WebRTCClient;
  private users: Map<string, UserPresence> = new Map();

  constructor(webrtc: WebRTCClient) {
    this.webrtc = webrtc;
    this.setupPresence();
  }

  private setupPresence(): void {
    // Send cursor position every 50ms
    document.addEventListener('mousemove', (e) => {
      this.webrtc.sendToAll({
        type: 'cursor-move',
        x: e.clientX,
        y: e.clientY
      });
    });

    // Handle cursor updates from others
    this.webrtc.on('cursor-move', (data: { userId: string; x: number; y: number }) => {
      const user = this.users.get(data.userId);
      if (user) {
        user.cursor = { x: data.x, y: data.y };
        user.lastSeen = Date.now();
        this.renderCursors();
      }
    });
  }

  private renderCursors(): void {
    const container = document.getElementById('cursors');
    if (!container) return;

    container.innerHTML = '';
    this.users.forEach((user) => {
      if (user.cursor) {
        const cursor = document.createElement('div');
        cursor.style.position = 'absolute';
        cursor.style.left = `${user.cursor.x}px`;
        cursor.style.top = `${user.cursor.y}px`;
        cursor.style.width = '20px';
        cursor.style.height = '20px';
        cursor.style.borderRadius = '50%';
        cursor.style.backgroundColor = user.color;
        cursor.innerHTML = `<span>${user.username}</span>`;
        container.appendChild(cursor);
      }
    });
  }
}
```

## Summary

P2P/WebRTC features:
- **WebRTC**: Peer-to-peer connections with STUN/TURN
- **Signaling**: Socket.io server for connection setup
- **Collaborative editing**: Real-time component changes
- **Screen sharing**: Share editor screen
- **Voice chat**: Real-time audio communication
- **Presence**: Live cursors and user status

**Effort:** 160 hours (4 weeks)

**Next:** Chapter 15 covers Plugin System architecture.

---

**Created by Manoj Jhawar, manoj@ideahq.net**
