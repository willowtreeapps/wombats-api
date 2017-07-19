An example bot written in JavaScript is:

```
((state, time_left) => ({
    command: {
        action: "turn",
        metadata: {
            direction: "right"
        }
    },
    state: {
        hello: "world"
    }
}));
```
