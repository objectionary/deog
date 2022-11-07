### Decoration graph of EO objects

DEOG takes a collection of `org.w3c.dom.Document` objects and constructs an inheritance graph, connecting all of them.
With this graph you can access the list of all the attributes present in an object in constant time.

#### Example:

Consider the following EO program.

```
[] > animal
  [] > live
    [] > eat
      stdout > @
        "Animal is eating"
    stdout > @
      "Animal is alive"
  [t] > talk
    live > @

[] > mouse
  animal > @
  [] > mouse_live
    ^.live > @

[] > cat
  animal > @
  [m] > meow
    ^.talk "Meow" > @
```

The resulting graph can be considered to look like below. Each node is represented by a rectangle. 
All the attributes of the node are listed in the rectangle.

![](assets/diag.drawio.png)

### API

To build a graph just use method `DeogLauncher.launch`.  
It returns a `Graph` object, which has a number of useful attributes, i.e.:
* `dgNodes` - a set of nodes of a graph
* `initialObjects` - a set of initial xml nodes of the documents

Nodes are represented by `DGraphNode` object with parameters such as 
* `name`
* `packageName`
* `attributes` - representing eo attributes
* `children` and `parents` in the graph
* `body` - corresponding `org.w3c.dom.Node`

`AttributesUtil` file provides a useful API for getting attributes of `org.w3c.dom.Node` objects.
