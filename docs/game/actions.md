# Game Actions

- [Game Overview](./)

The following are actions that can be taken during game play.

```clj
{:move-forward   {:description "Moves unit one space forward."}
 :move-backwards {:description "Moves unit one space backwards maintaining current orientation."}
 :turn-right     {:description "Rotates the units orientation 90° clockwise."}
 :turn-left      {:description "Rotates the units orientation 90° counter clockwise."}
 :shoot          {:description "Emits a flame from the unit"}
 :smoke          {:description "Emits smoke form the unit"}}
```

Actions are returned by your programs main function.

**TODO: Refine current messaging protocol and add documentation**
"
