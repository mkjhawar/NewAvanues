# Developer Manual - Chapter 67: Avanues Plugin Development

## Overview

Avanues is AVA's plugin ecosystem that enables third-party developers to extend AVA's capabilities. This chapter covers the plugin architecture, manifest format, and how to create plugins using MagicUI and MagicCode.

---

## Plugin Architecture

### Structure

```
{plugin_id}/
├── manifest.avp          # Plugin metadata & permissions
├── ui/
│   ├── main.ami          # Main screen MagicUI
│   └── components/       # Reusable UI components
├── code/
│   ├── main.amc          # Entry point MagicCode
│   └── handlers/         # Action handlers
├── intents/
│   └── plugin.ava        # Custom intents
└── assets/
    └── icons/            # Plugin icons
```

### Manifest Format (manifest.avp)

```yaml
schema: avp-1.0
plugin:
  id: com.avanues.{name}
  name: {Display Name}
  version: 1.0.0
  min_ava_version: 2.0.0
  author: {Author Name}
  description: {Plugin description}
permissions:
  - network
  - storage
entry_point: code/main.amc
ui_entry: ui/main.ami
```

---

## Available Permissions

| Permission | Description | Risk Level |
|------------|-------------|------------|
| network | Internet access | Medium |
| location | GPS/location services | High |
| camera | Camera access | High |
| microphone | Audio recording | High |
| storage | File system access | Medium |
| contacts | Contact list access | High |
| calendar | Calendar access | Medium |
| notifications | Send notifications | Low |
| bluetooth | Bluetooth control | Medium |
| smart_home | Smart home devices | Medium |

### Permission Request

```kotlin
// In plugin code
val granted = ava.requestPermission("location")
if (granted) {
    val location = ava.location.current()
}
```

---

## Plugin Lifecycle

| Event | Trigger | Use Case |
|-------|---------|----------|
| onInstall | First installation | One-time setup, DB creation |
| onEnable | Plugin enabled | Initialize resources |
| onDisable | Plugin disabled | Release resources |
| onUninstall | Before removal | Cleanup, data export prompt |
| onResume | App foreground | Refresh data |
| onPause | App background | Save state |

### Lifecycle Handlers

```
// In main.amc
EVT:onInstall:{storage.createTable("data",{...})}
EVT:onEnable:{ui.show("main_screen");refresh()}
EVT:onDisable:{saveState()}
EVT:onResume:{refresh()}
EVT:onPause:{saveState()}
```

---

## MagicUI for Plugins

### Main Screen (ui/main.ami)

```
---
schema: avu-1.0
type: ami
---
JSN:main_screen:Col#main{
  @pad:16;@bg:oceanDeep;spacing:16;
  Row#header{
    @align:spaceBetween;
    Text#title{text:"My Plugin";size:24;weight:bold;color:textPrimary};
    IconBtn#settings{icon:settings;color:textSecondary;onClick:openSettings}
  };
  LazyCol#content{
    flex:1;
    items:{dataList};
    itemTemplate:Card#item{@pad:12;@radius:8;@bg:surface10;...}
  };
  FAB#add{icon:add;color:coralBlue;onClick:addItem}
}
```

### Component Reuse

```
// ui/components/item_card.ami
JSN:item_card:Card#item{
  @pad:16;@radius:12;@bg:surface10;
  Row{spacing:12;@align:center;
    Icon{name:{icon};color:coralBlue};
    Col{flex:1;
      Text{text:{title};size:16;color:textPrimary};
      Text{text:{subtitle};size:12;color:textSecondary}
    };
    IconBtn{icon:chevron_right;color:textTertiary;onClick:{onTap}}
  }
}
```

---

## MagicCode for Plugins

### Data Models

```
// code/main.amc
DATA:model:Item:{
  id:String,
  title:String,
  description:String,
  createdAt:Long,
  completed:Boolean
}
DATA:list:items:Item[]
```

### Functions

```
FN:addItem:title:String:{
  storage.add("items", Item(uuid(), title, "", now(), false));
  refresh()
}

FN:deleteItem:id:String:{
  storage.remove("items", id);
  refresh()
}

FN:refresh::{
  items = storage.getAll("items");
  ui.update()
}
```

### Event Handlers

```
EVT:onEnable:{
  items = storage.getAll("items");
  ui.show("main_screen")
}

EVT:item_selected:id:{
  ui.navigate("detail_screen", {itemId: id})
}
```

### Actions

```
ACT:schedule:refresh:interval:15min
ACT:notification:reminder:{
  title:"Plugin Reminder",
  body:"Check your items"
}
```

---

## Custom Intents

### Plugin Intent File (intents/plugin.ava)

```yaml
---
schema: avu-1.0
version: 1.0.0
locale: en-US
metadata:
  file: plugin.ava
  plugin_id: com.avanues.myapp
---
NLU:plugin_my_action:open my plugin
NLU:plugin_my_action:show my items
NLU:plugin_add_item:add item {name}
NLU:plugin_add_item:new item called {name}
```

### Intent Registration

Plugins automatically register their intents when installed. The NLU system prefixes plugin intents with `plugin_` to avoid conflicts.

---

## API Access

### Storage API

```
// Create
storage.add("tableName", item)

// Read
storage.get("tableName", id)
storage.getAll("tableName")
storage.query("tableName", {where: "completed = false"})

// Update
storage.update("tableName", id, {field: value})

// Delete
storage.remove("tableName", id)
storage.clear("tableName")
```

### Network API

```
// GET request
api.get(url, {headers: {}})

// POST request
api.post(url, {body: {}, headers: {}})

// With auth
api.auth("bearer", token)
api.get(url)
```

### Location API

```
// Current location
location.current()  // {lat, lon, accuracy}

// Watch location
location.watch({interval: 5000}, callback)

// Stop watching
location.stopWatch()
```

### Notification API

```
// Show notification
notification.show({
  title: "Title",
  body: "Body text",
  icon: "icon_name",
  onClick: "handler_name"
})

// Schedule notification
notification.schedule({
  title: "Reminder",
  body: "Don't forget!",
  at: timestamp
})
```

---

## Example: Todo List Plugin

### manifest.avp

```yaml
schema: avp-1.0
plugin:
  id: com.avanues.todolist
  name: Todo List
  version: 1.0.0
  min_ava_version: 2.0.0
  author: AVA Developer
  description: Simple todo list manager
permissions:
  - storage
  - notifications
entry_point: code/main.amc
ui_entry: ui/main.ami
```

### ui/main.ami

```
---
schema: avu-1.0
type: ami
---
JSN:todo_screen:Col#todoScreen{
  @pad:16;@bg:oceanDeep;spacing:16;
  Row#header{
    @align:spaceBetween;
    Text#title{text:"My Todos";size:24;weight:bold;color:textPrimary};
    IconBtn#add{icon:add;color:coralBlue;onClick:addTodo}
  };
  LazyCol#todoList{
    items:{todos};
    itemTemplate:Row#item{
      @pad:12;@bg:surface10;@radius:8;spacing:12;
      Switch#check{checked:{completed};onToggle:toggleTodo:{id}};
      Text#text{text:{title};color:textPrimary;flex:1};
      IconBtn#delete{icon:delete;color:coralRed;onClick:deleteTodo:{id}}
    }
  }
}
```

### code/main.amc

```
---
schema: avu-1.0
type: amc
---
DATA:list:todos:Todo[]
DATA:model:Todo:{id:String,title:String,completed:Boolean,createdAt:Long}

FN:addTodo:title:String:{
  storage.add("todos", Todo(uuid(), title, false, now()));
  refresh()
}

FN:toggleTodo:id:String:{
  storage.update("todos", id, {completed: !completed});
  refresh()
}

FN:deleteTodo:id:String:{
  storage.remove("todos", id);
  refresh()
}

FN:refresh::{
  todos = storage.getAll("todos");
  ui.update()
}

EVT:onEnable:{
  todos = storage.getAll("todos");
  ui.show("todo_screen")
}
```

### intents/plugin.ava

```yaml
---
schema: avu-1.0
version: 1.0.0
locale: en-US
---
NLU:plugin_add_todo:add todo {item}
NLU:plugin_add_todo:new task {item}
NLU:plugin_add_todo:remind me to {item}
NLU:plugin_show_todos:show my todos
NLU:plugin_show_todos:what's on my list
NLU:plugin_clear_todos:clear completed todos
```

---

## Publishing Plugins

### Requirements

1. Valid manifest.avp
2. All permissions justified
3. No malicious code
4. Privacy policy (if using sensitive permissions)
5. Icon assets (48x48, 96x96, 192x192)

### Submission Process

1. Package plugin as .avp archive
2. Submit to Avanues Plugin Store
3. Automated security scan
4. Manual review (if using sensitive permissions)
5. Publication

---

## Best Practices

| Practice | Description |
|----------|-------------|
| Minimal permissions | Only request what you need |
| Graceful degradation | Handle permission denial |
| Offline support | Cache data locally |
| Error handling | Show user-friendly errors |
| Theme compliance | Use Ocean Glass tokens |
| Accessibility | Include content descriptions |

---

## Author

Manoj Jhawar
