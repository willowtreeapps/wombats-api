# Wombats Library for Clojure

## Overview
This is standard library for wombats, including functions that you may find helpful as a starting point or base for your own wombat. This library is not intended to include any complex logic or decision making -- it's just to help you get started with your wombat and remove the burden of common state manipulations you might you need.

#### All Functions

- [add-locs](#add-locs-arena)
- [add-to-state](#add-to-state-arena-elem)
- [build-command](#build-command-action)
- [build-initial-global-state](#build-initial-global-state-global-size)
- [build-resp](#build-resp-command)
- [can-shoot](#can-shoot-dir-arena-arena-size)
- [distance-to-tile](#distance-to-tile-dir-node-arena-size)
- [facing?](#facing-dir-target-arena-size)
- [filter-arena](#filter-arena-arena)
- [focus-sight](#focus-sight-arena)
- [front-tile](#front-tile-dir-arena-size)
- [get-arena-size](#get-arena-size-state)
- [get-direction](#get-direction-arena)
- [get-global-state](#get-global-state-state-path)
- [in?](#in-elem-coll)
- [is-clear?](#is-clear-arena-tile)
- [merge-global-state](#merge-global-state-global-arena-local-state-global-size)
- [move-to](#move-to-arena-arena-size-dir-loc)
- [new-direction](#new-direction-direction-loc-wombat-arena-size)
- [possible-points](#possible-points-arena)
- [select-target](#select-target-arena-arena-size)
- [turn-to-dir](#turn-to-dir-curr-dir-next-dir)

### Constants

```clojure
; There are 3 ways to turn
(def turn-directions [:right :left :about-face])
; There are 5 ways to throw a smoke bomb
(def smoke-directions [:forward :backward :left :right :drop])
; Game parameters taken from API itself
(def game-parameters
{;; HP Modifiers
 :collision-hp-damage 10
 :food-hp-bonus 5
 :poison-hp-damage 10
 ;; Score Modifiers
 :food-score-bonus 10
 :wombat-hit-bonus 10
 :zakano-hit-bonus 8
 :steel-barrier-hit-bonus 2
 :wood-barrier-hit-bonus 2
 :wombat-destroyed-bonus 25
 :zakano-destroyed-bonus 15
 :wood-barrier-destroyed-bonus 3
 :steel-barrier-destroyed-bonus 25
 ;; In game parameters
 :shot-distance 5})

; Pull out the number of tiles gunfire will travel
(def shot-range (:shot-distance game-parameters))

; The possible keys representing the 'type' of the 'contents' map within each tile
(def zakano-key "zakano")
(def wombat-key "wombat")
(def steel-key "steel-barrier")
(def wood-key "wood-barrier")
(def food-key "food")
(def poison-key "poison")
(def fog-key "fog")
(def open-key "open")

; Different lists of tile contents to be used as filters
(def point-sources [zakano-key wombat-key steel-key wood-key food-key])
(def points-no-wall [zakano-key wombat-key food-key])
(def enemies [wombat-key zakano-key])
(def blockers [zakano-key wombat-key wood-key steel-key poison-key])
(def targets [zakano-key wombat-key steel-key wood-key])

; The default tile used when creating the initial global state
(def default-tile {:contents {:type fog-key}})
```

### Functions

### `(add-locs arena)`
**This function takes in the matrix of maps representing the arena and annotates each tile with `:x`, and `:y` keys whose value correspond to the x and y coordinates of the tile within the matrix**
##### Params
- `arena`

  A 2 dimensional vector of maps representing the game arena
##### Usage
```clojure
; state is the top level parameter in wombat function
(def annotated-arena (add-locs (:arena state)))
```
##### Returns
Returns a deep copy of the arena matrix with each tile containing `:x` and `:y` keys

### `(add-to-state arena elem)`
**Takes in an arena and a single tile as a map and returns the arena with element at (x, y) replaced by the tile**
##### Params
- `arena`

  The two dimensional vector of maps representing the game arena

- `elem`

  A map representing a single tile of the arena; _MUST HAVE KEYS `:x` and `:y`_

##### Usage
```clojure
; state is the top level parameter in the wombat function
(def tile {:contents {:type "open"} :x 2 :y 4}

(def arena (add-to-state (:arena state) tile))
; Returns the arena with the dict at state.arena[4][2] replaced by tile
```
##### Returns
Modifies the arena in place as well as returning the new 2d vector representing the arena


### `(build-command action)`
### `(build-command action direction)`
**Helper function that will create a well formed response for a single wombat action**
##### Params
- `action`

  A string or keyword representing the action to be taken by your wombat

- `direction`

  For `:smoke` or `:turn`, a string or keyword representing the direction in which to take the action. Defaults to `nil`
##### Usage
```clojure
; Move forward
(def resp (build-command :move))

; Turn around
(def resp (build-command "turn" "about-face"))

; Throw smoke on top of yourself
(def resp (build-command :turn :about-face))

; Shoot directly in front of yourself
(def resp (build-command :shoot))

; Have your wombat actually perform the action
{:command resp}
; Or use (build_resp) to return
```
##### Returns
Returns a map containing the keys `:action` and `:metadata`


### `(build-initial-global-state global-size)`
**Constructs an initial global state populated by fog**
##### Params
- `global-size`

  A map with keys `:height` and `:width` representing the full size of the arena
##### Usage
```clojure
(def global-size {:height 20 :width 20})
; Global size will normally be obtained using get_arena-size()

(def global-arena (build-initial-global-state global-size))
```
##### Returns
Returns an 2 dimensional vector with `:height` rows where each row contains `:width` copies of `default_tile`


### `(build-resp command)`
### `(build-resp command state)`
**Helper function that will create a well formed response for your wombat function**
##### Params
- `command`

  The map with keys `:action` and `:metadata` defining the action to be taken by your wombat

- `state`

  A map representing the information you wish to save in the state map. Defaults to `{}`
##### Usage
```clojure
(def action (build-command :move)) ; However you want to construct your command

(build-resp action)
; Or
(def to-save {:did-turn true}) ; Whatever you want to save in state
(build-resp action to-save)
```
##### Returns
Returns a map containing the keys `:command` and `:state`

### `(can-shoot dir arena arena-size)`
### `(can-shoot dir arena arena-size :wall wall)`
### `(can-shoot dir arena arena-size :wombat wombat)`
### `(can-shoot dir arena arena-size :wombat wombat :wall wall)`
**Returns true if wombat shooting would damage an enemy or barrier**
##### Params
- `dir`
  
  The current direction the wombat is facing. One of `["n", "e", "s", "w"]`
- `arena`

  The two dimensional vector of maps representing the current state of the game arena
- `arena-size`

  A map containing keys `:height` and `:width` representing the size of the global arena
- `wombat`

  The current position of the player as a map. Default coordinated are (3, 3)

- `wall`

  A boolean flag representing whether `steel-barrier`s and `wood-barrier`s should be included in determining whether you can shoot

##### Usage
```clojure
; state is the top level parameter in the wombat function
(def global-size {:height 20 :width 20})
; Global size will normally be obtained using get_arena-size()
; Direction will normally be obtained using get_direction()
(def should-shoot (can-shoot "n" (:arena state) global-size))

; Fetch global_arena using get_global_state()
; You can find global coordinates using from state['global-coords']
(def should-shoot (can-shoot "e" global-arena global-size :wombat {:x 10 :y 4}))

; You might want to prioritize shooting wombats or zakanos over wood and steel barriers
(def should-shoot (can-shoot "n" (:arena state) global-size :wall false))
```

##### Returns
Returns `true` if there is a shootable tile directly in front of wombat -- wraparound included -- or `false` otherwise


### `(distance-to-tile dir node arena-size)`
### `(distance-to-tile dir node arena-size wombat)`
**Finds the minimum number of steps required to get from current position to node. Does not take into account actual pathfinding**
##### Params
- `dir`

  The current direction the wombat is facing. One of `["n", "e", "s", "w"]`
- `node`

  The target location as a map containing `:x` and `:y` keys representing location
- `arena-size`

  A map containing keys `:height` and `:width` representing the size of the global arena
- `wombat`

  The current position of the player as a map. Default coordinated are (3, 3)
##### Usage
```clojure
; state is the top level parameter in the wombat function
(def global-size {:height 20 :width 20})
; Global size will normally be obtained using get_arena-size()
; Direction will normally be obtained using get_direction()

(def dist (distance-to-tile "n" {:x 0 :y 0} global-size))
; Or
(def dist (distance-to-tile "e" {:x 12 :y 8} global-size {:x 10 :y 10}))
```
##### Returns
Return a the minimum number of turns to get to the given position as an integer


### `(facing? dir target arena-size)`
### `(facing? dir target arena-size wombat)`
**Determines if the player is currently facing target**
##### Params
- `dir`

  The current direction the wombat is facing. One of `["n", "e", "s", "w"]`
- `target`

  The target location as a map containing `:x` and `:y` keys representing location
- `arena-size`

  A map containing keys `:height` and `:width` representing the size of the global arena
- `wombat`

  The current position of the player as a map. Default coordinated are (3, 3)
##### Usage 
```clojure
(def global-size {:height 20 :width 20})
; Global size will normally be obtained using get_arena-size()

(def facing-top-left (facing? "n" {:x 0 :y 0} global-size)) ; true
(def facing-top-left (facing? "w" {:x 0 :y 0} global-size)) ; true
(def facing-top-left (facing? "e" {:x 0 :y 0} global-size)) ; false

(def facing (facing? "s" {:x 11 :y 6} global-size {:x 17 :y 19})) ; false
```
##### Returns
Returns `true` if a move forward would bring wombat closer to target, arena wraparound included, `false` otherwise


### `(filter-arena arena)`
### `(filter-arena arena filters)`
**Takes in a 2 dimensional vector representing the arena and returns a list of tiles of a type in `filters`**
##### Params
- `arena`

  The two dimensional vector of maps representing the current state of the game arena

- `filters`

  An vector of strings where each string could be the contents of an arena tile
  Default to `nil`
##### Usage
```clojure
; state is the top level parameter in the wombat function
(def arena (add-locs (:arena state)))

; When filters=nil, return only the flattened arena without removing any contents
(def all-tiles (filter-arena arena))

(def all-enemies (filter-arena arena ["wombat" "zakano"]))
```
##### Returns
Returns a single dimensional vector of maps where the content type is in `filters`, or all tiles whe `filters` is `nil`

### `(focus-sight arena)`
**Cuts down the 7x7 local arena to a 5x5 arena to focus on maps closer to the player**
##### Params
- `arena`

  The two dimensional (7x7) vector of maps representing the current state of the game arena

##### Usage
```clojure
; state is the top level parameter in the wombat function
(def arena (add-locs (:arena state)))

; There are some situations where you would rather look for only tile contents within 2 block range, rather than 3
(def small-view (focus-sight arena))
```
##### Returns
Returns a two dimensional (5x5) vector of maps with (2, 2) representing the center of the 7x7 vector

### `(front-tile dir arena-size)`
### `(front-tile dir arena-size wombat)`
**Gets the tile directly in front of the given wombat**
###### Params
- `dir`
  
  The current direction the wombat is facing. One of `["n", "e", "s", "w"]`

- `arena-size`

  A map with keys `:height` and `:width` representing the full size of the arena

- `wombat`

  The current position of the player as a map. Default coordinated are (3, 3)
##### Usage
```clojure
(def arena-size {:height 20 :width 20})

(def next-tile (front-tile "e" arena-size))
; next_tile = {x: 4, y: 3}
; Or
(def next-tile (front-tile "n" arena-size {:x 3 :y 0}))
; next_tile = {x: 3, y: 19}
```
##### Returns
Returns a dictions with keys `:x` and `:y`

### `(get-arena-size state)`
**Gets the `arena-size` map**
##### Params
- `state`

  The top level parameter of the wombat function, a map with the key `'global-dimensions'`
##### Usage
```clojure
; state is the top level parameter in the wombat function
(def arena-size (get-arena-size state))
```
##### Returns
Returns a map with the keys `:height` and `:width`

### `(get-direction arena)`
**Get the current direction of the player wombat**
##### Params
- `arena`

  The two dimensional vector of maps representing the current state of the game arena
##### Usage
```clojure
; state is the top level parameter in the wombat function
(def current-direction (get-direction (:arena state)))
```
##### Returns
Returns a strings that is one of `"n"`, `"e"`, `"s"`, or `"w"`

### `(get-global-state state path)`
**Attempts to get the global saved state or create a new one if it isn't stored in the saved state**
##### Params
- `state`

  The top level parameter of the wombat function

- `path`

  An vector of keywords representing the path of keys from the top level state map to the saved global state
##### Usage
```clojure
; state is the top level parameter in the wombat function
(def global-state (get-global-state state [:saved-state :global : arena]))
; Update the global state and do things with it

; Use build_resp() and build_command() to simplifiy the return statement
{ :command resp ; Whatever action you are taking
  :state { :global { :arena global-state}}}
```

### `(in? elem coll)`
**Helper function to determine if elem is in coll**
##### Params
- `elem`

  Any element you want to check
- `coll`

  Any collection you want to see if it contains elem
##### Usage
```clojure
(in? "a" ["a" "b" "c"]) ; true
(in? :a [:b :c]) ; false
```
##### Returns
Returns `true` if collection contains elem, `false` otherwise


### `(is-clear? arena tile)`
**Tells you whether given tile can be safely occupied or not**
##### Params
- `arena`

  The two dimensional vector of maps representing the current state of the game arena
- `tile`

  The target location as a map containing `:x` and `:y` keys representing location
##### Usage
```clojure
; state is the top level parameter in the wombat function
; Arbitrary tile
(def front-tile {:x 2 :y 3})

(def can-move (is-clear (:arena state) front-tile))
```
##### Returns
Returns a `true` if wombat can safely occupy given tile, `false` otherwise


### `(merge-global-state global-arena local-state global-size)`
**Add all information from local state to global state**
##### Params
- `global-arena`
  
  A two dimensional vector of maps representing the global arena
- `local-state`

  The top level parameter of the wombat function
- `global-size`

  A map containing keys `:height` and `:width` representing the size of the global arena
##### Usage
```clojure
; state is the top level parameter in the wombat function
(def global-size {:height 20 :width 20})
; Global size will normally be obtained using get_arena-size()

(def global-arena (get-global-state state [:saved-state :arena]))

(def updated-global-arena (merge-global-state global-arena state global-size))
```
##### Returns
Returns the global arena with each persistent tile in the local arena overwritting the corresponding tile in the global arena


### `(move-to arena arena-size dir loc)`
### `(move-to arena arena-size dir loc wombat)`
**Pick a move that would move you closer to target location**
##### Params
- `arena`

  The two dimensional vector of maps representing the current state of the game arena
- `arena-size`

  A map containing keys `:height` and `:width` representing the size of the global arena
- `dir`
  
  The current direction the wombat is facing. One of `['n', 'e', 's', 'w']`
- `loc`

  The target location as a map containing `:x` and `:y` keys representing location
- `wombat`

  The current position of the player as a map. Defaults to (3, 3)
##### Usage
```clojure
; state is the top level parameter in the wombat function
(def global-size {:height 20 :width 20})
; Global size will normally be obtained using get_arena-size()

; Arbitrary tiles
(def enemy {:x 5 :y 1})
(def wombat {:x 3 :y 6})

(def next-move (move-to (:arena state) global-size "e" enemy wombat))
; Or
(def next-move (move-to (:arena state) global-size "e" enemy))
```
##### Returns
Returns a map representing the action for the wombat to take including keys `:action` and `:metadata`.
Returns `nil` if no way to safely move towards location


### `(new-direction direction loc wombat arena-size)`
**Find which way to turn to face given location**
- `direction`
  
  The current direction the wombat is facing. One of `["n", "e", "s", "w"]`
- `loc`

  The target location as a map containing `:x` and `:y` keys representing location
- `wombat`

  The current position of the player as a map.
- `arena-size`

  A map containing keys `:height` and `:width` representing the size of the global arena
##### Usage
```clojure
(def global-size {:height 20 :width 20})
; Global size will normally be obtained using get_arena-size()

; Arbitrary tile
(def enemy {:x 5 :y 1})

(def new-dir (new-direction "n" enemy {:x 3 :y 1} global-size)) ; :right
(def new-dir (new-direction "w" enemy {:x 3 :y 1} global-size)) ; :about-face
(def new-dir (new-direction "e" enemy {:x 3 :y 1} global-size)) ; nil
```
##### Returns
Returns one of `:left`, `:about-face`, `:right`, or `nil`


### `(possible-points arena)`
### `(possible-points arena :wall wall)`
### `(possible-points arena :wombat wombat )`
### `(possible-points arena :wombat wombat :wall wall)`
**Get a list of all possible point sources within provided arena**
##### Params
- `arena`

  The two dimensional vector of maps representing the current state of the game arena
- `wombat`

  The current position of the player as a map.
- `wall`

  A boolean flag representing whether `steel-barrier`s and `wood-barrier`s should be included in the possible point sources
##### Usage
```clojure
; state is the top level parameter in the wombat function
(def arena (:arena state))

(def possible-targets (possible-points arena))
; Or
(def possible-targets (possible-points arena :wall false))

; If using the global arena instead of local arena, provide wombat location so that player is filtered out
(def global-arena (get-in state [:saved-state :arena])) ; However you get your global_arena

(def wombat {:x 13 :y 8}) ; Get your global position from state

(def possible-targets (possible-points global-arena :wombat wombat))
; Or
(def possible-targets (possible-points global-arena :wombat wombat :wall false))

```
##### Returns
Returns a list of maps where each map is an arena tile

### `(select-target arena arena-size)`
### `(select-target arena arena-size :wall wall)`
### `(select-target arena arena-size :wombat wombat)`
### `(select-target arena arena-size :wombat wombat :wall wall)`
**Get the closest point source to the player**
##### Params
- `arena`

  The two dimensional vector of maps representing the current state of the game arena
- `arena-size`

  A map containing keys `:height` and `:width` representing the size of the global arena
- `wombat`

  The current position of the player as a map.
- `wall`

  A boolean flag representing whether `steel-barrier`s and `wood-barrier`s should be included in the possible point sources
##### Usage
```clojure
; state is the top level parameter in the wombat function
(def global-size {:height 20 :width 20})
; Global size will normally be obtained using get_arena-size()

(def arena (:arena state))

(def target (select-target arena arena-size))
; Or
(def target (select-target arena arena-size :wall false))

; If using the global arena instead of local arena, provide wombat location so that player is filtered out
(def global-arena (get-in state [:saved-state :arena])) ; However you get your global-arena
(def wombat {:x 13 :y 7}) ; Get your global position from state

(def target (select-target global-arena arena-size :wombat wombat))
; Or
(def target (select-target global-arena arena-size :wombat wombat :wall wall))
```
##### Returns
Returns a map representing an arena tile with keys `:content`, `:x`, and `:y`


### `(turn-to-dir curr-dir next-dir)`
**Gives the action needed to turn from `curr_dir` to `next_dir`**
##### Params
- `curr_dir`

  A string representing the current direction the wombat is facing. One of `"n"`, `"e"`, `"s"`, `"w"`
- `next_dir`

  A string representing the desired direction for the wombat to face. One of `"n"`, `"e"`, `"s"`, `"w"`

##### Usage
```clojure
(def direction (turn-to-dir "s" "n")) ; :about-face
(def direction (turn-to-dir "e" "n")) ; :left
(def direction (turn-to-dir "w" "n")) ; :right
(def direction (turn-to-dir "n" "n")) ; nil
```
##### Returns
Returns one of `:left`, `:about-face`, `:right`, or `nil`

## More Info
For more information about any of the functions or their usage, or if you'd like to use them as the starting point for any of your own wombat, view the [source code](wombat_lib.clj)
