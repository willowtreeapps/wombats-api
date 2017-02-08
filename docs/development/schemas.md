# Wombats Schemas

- [Development Home](./)

### Client Schemas

## Arena

```clj
;; [[Cell, Cell, Cell, ...]
;;  [Cell, Cell, Cell, ...]
;;  ...]
```

## Cell

```clj
{:cell-type :type
 :cell-properties {}}
```

#### Cell Types

```clj
:wombat          ;; Human player
:zakano          ;; Enemy AI
:wood-barrier    ;; Barrier
:steel-barrier   ;; Indestructible Barrier
:flame           ;; Flame
:smoke           ;; Player / AI Smoke
:fog             ;; Map Fog
:food            ;; Food
:poison          ;; Poison
:open            ;; Open Space
```

#### Cell Properties

**wombat**

```clj
{:hp          100
 :orientation :n
 :username    "oconn"
 :color       "#454AB3"}
```

**zakano**

```clj
{:hp          100
 :orientation :n}
```

**wood-barrier**

```clj
{:percent-decay 10}
```

**steel-barrier**

```clj
{}
```

**flame**

```clj
{:orientation :n}
```

**smoke**

```clj
{}
```

**fog**

```clj
{}
```

**food**

```clj
{}
```

**poison**

```clj
{}
```

**open**

```clj
{}
```
