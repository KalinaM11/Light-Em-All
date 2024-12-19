import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// to represent a LightEmAll game
class LightEmAll extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  // the current location of the power station,
  int powerRow;
  int powerCol;
  // effective radius of power station
  // int radius;
  Random rand;

  int countClicks;

  // generates a functional LightEmAll game given only a width and height
  LightEmAll(int width, int height) {
    this.width = width;
    this.height = height;
    this.rand = new Random();
    this.board = new Utils().makeBoard(width, height);
    this.nodes = new Utils().listNode(this.board);
    this.mst = this.kruskal();
    this.powerRow = 0;
    this.powerCol = 0;
    board.get(0).get(0).powerStation = true;
    new Utils().boardComplete(this.mst);
    this.shuffle(new Random());
    this.bfs();
    this.countClicks = 0;
    // this.radius = radius;
  }

  // constructor for testing 
  LightEmAll(ArrayList<ArrayList<GamePiece>> board, Random rand) {
    this.board = board;
    this.width = this.board.size();
    this.height = this.board.get(0).size();
    this.rand = rand;
    this.nodes = new Utils().listNode(this.board);
    this.mst = this.kruskal();
    this.powerRow = 0;
    this.powerCol = 0;
    board.get(0).get(0).powerStation = true;
    new Utils().boardComplete(this.mst);
    this.shuffle(rand);
    this.bfs();
    this.countClicks = 0;
    // this.radius = radius;
  }

  // constructor for testing
  LightEmAll(ArrayList<ArrayList<GamePiece>> board) {
    this.rand = new Random();
    this.board = board;
    this.nodes = new Utils().listNode(board);
    this.mst = new ArrayList<Edge>();
    this.width = this.board.size();
    this.height = this.board.get(0).size();
    this.powerRow = new Utils().powerLocation("row", board);
    this.powerCol = new Utils().powerLocation("col", board);
    this.countClicks = 0;
  }

  //checks for boundaries, and checks that two pieces are connected
  // given a String to represent which tile is being checked
  boolean checkPieces(String s, GamePiece g) {
    return (s.equals("up") && g.top && g.row > 0 
        && g.connected(this.board.get(g.col).get(g.row - 1)) 
        || (s.equals("down") && g.bottom && g.row != height - 1 
        && g.connected(this.board.get(g.col).get(g.row + 1))) 
        || (s.equals("left") && g.left && g.col > 0 
            && g.connected(this.board.get(g.col - 1).get(g.row)))
        || (s.equals("right") && g.right && g.col < width - 1 
            && g.connected(this.board.get(g.col + 1).get(g.row))));
  }


  // EFFECT: powers nodes which are connected to the powerStation
  void bfs() {
    // adds the powerStation to the beenAt list 
    ArrayList<GamePiece> beenAt = new ArrayList<GamePiece>(
        Arrays.asList(this.board.get(powerCol).get(powerRow)));

    // resets the board so nothing is connected
    for (GamePiece gp : this.nodes) {
      gp.powered = false;
    }
    // lights up the powerStation
    this.board.get(powerCol).get(powerRow).powered = true;

    // for every game piece that we've been at, checks if neighboring pieces are 
    // connected and should be lit up 
    for (int i = 0; i < beenAt.size(); i++) {
      GamePiece g = beenAt.get(i);
      if (g.right && g.col < this.width - 1) {
        GamePiece rightTile = this.board.get(g.col + 1).get(g.row);
        if (!(beenAt.contains(rightTile)) && g.connected(rightTile)) {
          rightTile.powered = true;
          beenAt.add(rightTile);
        }
      }
      if (g.left && g.col > 0) {
        GamePiece leftTile = this.board.get(g.col - 1).get(g.row);
        if (!(beenAt.contains(leftTile)) && g.connected(leftTile)) {
          leftTile.powered = true;
          beenAt.add(leftTile);
        }
      }
      if (g.bottom && g.row != height - 1) {
        GamePiece botTile = this.board.get(g.col).get(g.row + 1);
        if (!(beenAt.contains(botTile)) && g.connected(botTile)) {
          botTile.powered = true;
          beenAt.add(botTile);
        }
      }
      if (g.top && g.row > 0) {
        GamePiece topTile = this.board.get(g.col).get(g.row - 1);
        if (!(beenAt.contains(topTile)) && g.connected(topTile)) {
          topTile.powered = true;
          beenAt.add(topTile);
        }
      }
      // check if the game is won
      this.gameWon();
    }
  }

  // returns true if all nodes are powered
  boolean gameWon() {
    ArrayList<GamePiece> poweredList = new ArrayList<GamePiece>();
    for (GamePiece gp : this.nodes) {
      if (gp.powered) {
        poweredList.add(gp);
      }
    }
    return (poweredList.size() == this.nodes.size());
  }

  // to represent the win screen
  public WorldScene lastScene(String s) {
    WorldScene w = makeScene();
    w.placeImageXY(new TextImage(s, 40, Color.magenta), (this.width * 60) / 2,  
        (this.height * 60) / 2);
    return w;
  }

  // draws the scene 
  public WorldScene makeScene() {
    WorldScene w = new WorldScene(this.width * 60, this.height * 60);
    w.placeImageXY(new RectangleImage(this.width * 60, this.height * 60 + 100, 
        OutlineMode.SOLID, Color.gray), this.width * 30, this.height * 60);
    int x = 30;
    for (int c = 0; c < this.board.size(); c++) {
      int y = 30;
      for (int r = 0; r < this.board.get(c).size(); r++) {
        GamePiece tile = this.board.get(c).get(r);
        if (tile.powered) {
          w.placeImageXY(tile.tileImage(60, 10, Color.yellow, tile.powerStation), x, y);
        }
        else {
          w.placeImageXY(tile.tileImage(60, 10, Color.gray, tile.powerStation), x, y);
        }
        y += 60;
      }
      x += 60;
    }

    OverlayOffsetImage clickCounter = new OverlayOffsetImage(new TextImage("Clicks", 
        15, Color.black), 0, 45 ,new OverlayOffsetImage(new TextImage(Integer
            .toString(this.countClicks), 30, Color.white), 0, 0, new RectangleImage(75, 50, 
                OutlineMode.SOLID, Color.DARK_GRAY)));
    w.placeImageXY(clickCounter, (this.width * 60) / 2, this.height * 60 + 50);
    return w;
  }

  //EFFECT: rotates a GamePiece
  public void rotate(GamePiece g) {
    GamePiece rotated = new GamePiece(g.col, g.row, 
        false, false, false, false, g.powerStation, g.powered);
    if (g.left) {
      rotated.top = true;
    }
    if (g.right) {
      rotated.bottom = true;
    }
    if (g.top) {
      rotated.right = true;
    }
    if (g.bottom) {
      rotated.left = true;
    }
    g.top = rotated.top;
    g.bottom = rotated.bottom;
    g.left = rotated.left;
    g.right = rotated.right;
  }


  //EFFECT: rotates the piece at the clicked position
  public void onMouseClicked(Posn pos) {
    if (pos.y <= this.height * 60 ) {
      int colPosn = 0;
      int rowPosn = 0;
      if (pos.x >= 0 && pos.x <= this.width * 60 && pos.y >= 0 && pos.y <= this.height * 60) {
        for (int i = 0; i < this.width; i++) {
          if ((i * 60) <= pos.x && pos.x < ((i * 60) + 60)) {
            colPosn = i;
          }
        }
        for (int j = 0; j < this.height; j++) {
          if ((j * 60) < pos.y && pos.y <= ((j * 60) + 60)) {
            rowPosn = j;
          }
        }
      }
      this.rotate(this.board.get(colPosn).get(rowPosn));
      this.bfs();
      // if there are too many clicks 
      if (this.countClicks == this.nodes.size() * 4) {
        this.endOfWorld("GAME OVER!");
      }
      // if game is won
      if (this.gameWon()) {
        this.endOfWorld("YOU WON!");
      }
      // increment the clicks
      this.countClicks++;
    }
  }

  // EFFECT: shuffles the nodes
  void shuffle(Random r) {
    for (GamePiece g : this.nodes) {
      for (int i =  0; i < r.nextInt(4); i++) {
        this.rotate(g);
      }
    }
  }

  // EFFECT: on the key pressed, moves the powerStation
  public void onKeyEvent(String s) {
    GamePiece ps = board.get(powerCol).get(powerRow);
    // if you click r a new game opens 
    if (s.equals("r")) {
      this.endOfWorld("RESET");
      new LightEmAll(this.width, this.height).bigBang(this.width * 60, this.height * 60 + 100);
    }

    if (s.equals("up") && this.checkPieces(s, ps)) {
      GamePiece topTile = this.board.get(ps.col).get(ps.row - 1);
      ps.powerStation = false;
      topTile.powerStation = true;
      this.powerRow = topTile.row;
      this.powerCol = topTile.col;
    }
    if (s.equals("down") && this.checkPieces(s, ps)) {
      GamePiece botTile = this.board.get(ps.col).get(ps.row + 1);
      ps.powerStation = false;
      botTile.powerStation = true;
      this.powerRow = botTile.row;
      this.powerCol = botTile.col;
    }
    if (s.equals("left") && this.checkPieces(s, ps)) {
      GamePiece leftTile = this.board.get(ps.col - 1).get(ps.row);
      ps.powerStation = false;
      leftTile.powerStation = true;
      this.powerRow = leftTile.row;
      this.powerCol = leftTile.col;
    }
    if (s.equals("right") && this.checkPieces(s, ps)) {
      GamePiece rightTile = this.board.get(ps.col + 1).get(ps.row);
      ps.powerStation = false;
      rightTile.powerStation = true;
      this.powerRow = rightTile.row;
      this.powerCol = rightTile.col;
    }
  }

  // creates all the possible edges in a game
  ArrayList<Edge> loEdges(Random rand) {
    ArrayList<Edge> allEdges = new ArrayList<Edge>();
    for (int c = 0; c < this.board.size(); c++) {
      for (int r = 0; r < this.board.get(c).size(); r++) {
        if (c < this.board.size() - 1) {
          Edge e = new Edge(this.board.get(c).get(r), this.board.get(c + 1).get(r), 
              rand.nextInt());
          allEdges.add(e);
        }
        if (r < this.board.get(c).size() - 1) {
          Edge e = new Edge(this.board.get(c).get(r), this.board.get(c).get(r + 1), 
              rand.nextInt());
          allEdges.add(e);
        }
      }
    }
    return allEdges; 
  } 

  // finds the representative of a given game piece 
  GamePiece find(HashMap<GamePiece, GamePiece> hm, GamePiece key) {
    if (hm.get(key).equals(key)) {
      return key;
      // this is the representative 
    }
    else {
      // find the representative 
      return find(hm, hm.get(key));
    }
  }

  // EFFECT: unions together two game pieces 
  void union(HashMap<GamePiece, GamePiece> representatives, GamePiece gp1,
      GamePiece gp2) {
    representatives.put(gp1, gp2);

  }

  // uses kruskal algorithm to randomize the board configuration
  ArrayList<Edge> kruskal() {
    HashMap<GamePiece, GamePiece> representatives = new HashMap<GamePiece, GamePiece>();
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    ArrayList<Edge> worklist = new Utils().sortEdges(this.loEdges(this.rand));
    // makes every node it's own representative 
    for (GamePiece gp: this.nodes) {
      representatives.put(gp, gp);
    }
    // while there are more trees in the forest keep adding new edges
    while (edgesInTree.size() < (this.height * this.width - 1)) {
      Edge nextEdge = worklist.remove(0);
      if (this.find(representatives, nextEdge.toNode)
          .equals(this.find(representatives, nextEdge.fromNode))) {
        // this would create a cycle so discard 
      }
      else {
        // update the representatives and add the edge to the tree
        edgesInTree.add(nextEdge);
        union(representatives, find(representatives, nextEdge.toNode),
            find(representatives, nextEdge.fromNode));
      }
    }

    return edgesInTree;
  }
}

/// represents a tile on the board 
class GamePiece {
  int col;
  int row;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;
  // whether the power station is on this piece
  boolean powerStation;
  // whether this GamePiece is powered
  boolean powered;

  GamePiece(int col, int row, boolean left, boolean right, boolean top, boolean bottom, 
      boolean powerStation, boolean powered) {
    this.col = col;
    this.row = row;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.powered = powered;
  }

  // Generate an image of this, the given GamePiece.
  WorldImage tileImage(int size, int wireWidth, Color wireColor, boolean hasPowerStation) {
    WorldImage image = new OverlayImage(
        new RectangleImage(wireWidth, wireWidth, OutlineMode.SOLID, wireColor),
        new RectangleImage(size, size, OutlineMode.SOLID, Color.DARK_GRAY));
    WorldImage vWire = new RectangleImage(wireWidth, (size + 1) / 2, OutlineMode.SOLID, wireColor);
    WorldImage hWire = new RectangleImage((size + 1) / 2, wireWidth, OutlineMode.SOLID, wireColor);

    if (this.top) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, 0, image);
    } 
    if (this.right) {
      image = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    if (this.bottom) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, vWire, 0, 0, image);
    }
    if (this.left) {
      image = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    if (hasPowerStation) {
      image = new OverlayImage(
          new OverlayImage(
              new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
              new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))),
          image);
    }
    image = new OverlayImage(new RectangleImage(size, size, OutlineMode.OUTLINE, 
        Color.BLACK), image);
    return image;
  }

  // checks if this GamePiece and the given GamePiece are connected 
  boolean connected(GamePiece gp) {
    // top neighbor
    if ((gp.row == this.row - 1) && gp.col == this.col) {
      return (gp.bottom && this.top);
    }
    // bottom neighbor
    if ((gp.row == this.row + 1) && gp.col == this.col) {
      return (gp.top && this.bottom);
    }
    // left neighbor
    if ((gp.row == this.row) && gp.col == this.col - 1) {
      return (gp.right && this.left);
    }
    // right neighbor
    if ((gp.row == this.row) && gp.col == this.col + 1) {
      return (gp.left && this.right);
    }
    else {
      return false;
    }
  }
}

// represents an edge 
class Edge {
  GamePiece fromNode;
  GamePiece toNode;
  int weight;

  public Edge(GamePiece fromNode, GamePiece toNode) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = new Random().nextInt();
  }

  public Edge(GamePiece fromNode, GamePiece toNode, int weight) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = weight;
  }

  Edge smaller(Edge e) {
    if (this.weight <= e.weight) {
      return this;
    }
    else {
      return e;
    }
  }
}

class Utils {

  // makes a list of nodes from the board
  ArrayList<GamePiece> listNode(ArrayList<ArrayList<GamePiece>> board) {
    ArrayList<GamePiece> nodeList = new ArrayList<GamePiece>();
    for (ArrayList<GamePiece> gp: board) {
      nodeList.addAll(gp);
    }
    return nodeList;
  }

  //returns column or row location of powerRow
  int powerLocation(String s, ArrayList<ArrayList<GamePiece>> board) {
    int loc = -1;
    for (int c = 0; c < board.size(); c++) {
      for (int r = 0; r < board.get(c).size(); r++) {
        if (board.get(c).get(r).powerStation) {
          if (s.equals("row")) {
            loc = r;
          }
          if (s.equals("col")) {
            loc = c;
          }
        }
      }
    }
    if (loc == -1) {
      throw new IllegalArgumentException("No PowerStation");
    }
    else {
      return loc;
    }
  }

  // generates the board
  ArrayList<ArrayList<GamePiece>> makeBoard(int width, int height) {
    if (width < 2 || width > 20 || height < 2 || height > 14) {
      throw new IllegalArgumentException("Invalid height or width");
    }
    ArrayList<ArrayList<GamePiece>> board = new ArrayList<ArrayList<GamePiece>>();
    for (int c = 0; c < width; c++) {
      ArrayList<GamePiece> column = new ArrayList<GamePiece>();
      for (int r = 0; r < height; r++) {
        column.add(new GamePiece(c, r, false, false, false, false, false, false));
      }
      board.add(column);
    }
    return board;
  }

  // EFFECT: draws the game piece configurations 
  void boardComplete(ArrayList<Edge> edges) {
    for (int i = 0; i < edges.size(); i++) {
      GamePiece from = edges.get(i).fromNode;
      GamePiece to = edges.get(i).toNode;
      int fromRow = from.row;
      int fromCol = from.col;
      int toRow = to.row;
      int toCol = to.col;
      if (fromRow < toRow) {
        from.right = true;
        to.left = true;
      }
      if (fromCol < toCol) {
        from.bottom = true;
        to.top = true;
      }
      else if (fromRow > toRow) {
        from.left = true;
        to.right = true;
      }
      else if (fromCol > toCol) {
        from.top = true;
        to.bottom = true;
      } 
    }
  }

  //EFFECT: generates the different pieces 
  void makePieces(ArrayList<ArrayList<GamePiece>> board, int powerRow, int powerCol) {
    for (int c = 0; c < board.size(); c++) {
      for (int r = 0; r < board.get(c).size(); r++) {
        if (r < board.get(c).size() - 1) {
          board.get(c).get(r).bottom = true;
        }
        if (r != 0) {
          board.get(c).get(r).top = true;
        }
        if (r == powerRow && c < board.size() - 1) {
          board.get(c).get(r).right = true;
        }
        if (r == powerRow && c > 0) {
          board.get(c).get(r).left = true;
        }
        if (r == powerRow && c == powerCol) {
          board.get(c).get(r).powerStation = true;
        }
      }
    }
  }

  // uses insertions sort to sort the edges based on increasing length
  ArrayList<Edge> sortEdges(ArrayList<Edge> arr) {
    for (int j = 0; j < arr.size(); j++) {
      Edge currLow = arr.get(j);
      for (int i = j + 1; i < arr.size(); i++) {
        currLow = arr.get(i).smaller(currLow);
      }
      int indexLow = arr.indexOf(currLow);
      Collections.swap(arr, j, indexLow);
    }
    return arr;
  }

}

class ExamplesLightEmAll {
  ExamplesLightEmAll() {} 

  // in first column
  GamePiece gp1;
  GamePiece gp2;
  GamePiece gp3;
  GamePiece gp4;
  GamePiece gp5;
  GamePiece gp6;
  GamePiece gp7;
  GamePiece gp8;
  GamePiece gp9;
  GamePiece gp10;
  GamePiece gp11;
  GamePiece gp12;
  GamePiece gp13;
  GamePiece gp14;
  GamePiece gp15;
  GamePiece gp16;

  // in a 2  by 2
  GamePiece gp01;
  GamePiece gp02;
  GamePiece gp03;
  GamePiece gp04;

  GamePiece gpa;
  GamePiece gpb;
  GamePiece gpc;
  GamePiece gpd;

  ArrayList<GamePiece>  col0;
  ArrayList<GamePiece>  col1;
  ArrayList<GamePiece>  col2;
  ArrayList<GamePiece>  col3;

  ArrayList<GamePiece> node1;

  ArrayList<ArrayList<GamePiece>> board1;
  ArrayList<ArrayList<GamePiece>> board2;
  ArrayList<ArrayList<GamePiece>> board3;

  LightEmAll game1;
  LightEmAll game2;
  LightEmAll game3;
  LightEmAll game4;

  Edge e1;
  Edge e2;
  Edge e3;
  Edge e4;
  Edge e5;

  Edge e01;
  Edge e02;
  Edge e03;
  Edge e04;

  ArrayList<Edge> edges1;
  ArrayList<Edge> edges2;
  ArrayList<Edge> edges3;
  ArrayList<Edge> edges4;
  ArrayList<Edge> edges5;

  Random r;
  Random r2;

  HashMap<GamePiece, GamePiece> hm1;

  void reset() {

    r = new Random(10);
    r2 = new Random(5);
    // first column
    gp1 = new GamePiece(0, 0, false, false, false, true, false, false);
    gp2 = new GamePiece(0, 1, false, false, true, true, false, false);
    gp3 = new GamePiece(0, 2, false, true, true, true, false, false);
    gp4 = new GamePiece(0, 3, false, false, true, false, false, false);
    // second column
    gp5 = new GamePiece(1, 0, false, false, false, true, false, false);
    gp6 = new GamePiece(1, 1, false, false, true, true, false, false);
    gp7 = new GamePiece(1, 2, true, true, true, true, false, false);
    gp8 = new GamePiece(1, 3, false, false, true, false, false, false);
    // third column
    gp9 = new GamePiece(2, 0, false, false, false, true, false, false);
    gp10 = new GamePiece(2, 1, false, false, true, true, false, false);
    // powerStation
    gp11 = new GamePiece(2, 2, true, true, true, true, true, true);
    gp12 = new GamePiece(2, 3, false, false, true, false, false, false);
    // fourth column
    gp13 = new GamePiece(3, 0, false, false, false, true, false, false);
    gp14 = new GamePiece(3, 1, false, false, true, true, false, false);
    gp15 = new GamePiece(3, 2, true, false, true, true, false, false);
    gp16 = new GamePiece(3, 3, false, false, true, false, false, false);

    // for 2 by 2 grid 
    // first column
    gp01 = new GamePiece(0, 0, false, false, false, false, true, true);
    gp02 = new GamePiece(0, 1, false, false, false, false, false, false);
    // second column
    gp03 = new GamePiece(1, 0, false, false, false, false, false, false);
    gp04 = new GamePiece(1, 1, false, false, false, false, false, false);

    // another 2 by 2 grid
    gpa = new GamePiece(0, 0, false, false, false, false, true, true);
    gpb = new GamePiece(0, 1, false, false, false, false, false, false);
    gpc = new GamePiece(1, 0, false, false, false, false, false, false);
    gpd = new GamePiece(1, 1, false, false, false, false, false, false);

    // list of columns 
    col0 = new ArrayList<GamePiece>(Arrays.asList(this.gp1, this.gp2, this.gp3, this.gp4));
    col1 = new ArrayList<GamePiece>(Arrays.asList(this.gp5, this.gp6, this.gp7, this.gp8));
    col2 = new ArrayList<GamePiece>(Arrays.asList(this.gp9, this.gp10, this.gp11, this.gp12));
    col3 = new ArrayList<GamePiece>(Arrays.asList(this.gp13, this.gp14, this.gp15, this.gp16));

    // list of all nodes in a game 
    node1 = new ArrayList<GamePiece>(Arrays.asList(this.gp1, this.gp2, this.gp3, this.gp4, 
        this.gp5, this.gp6, this.gp7, this.gp8, this.gp9, this.gp10, this.gp11, this.gp12, 
        this.gp13, this.gp14, this.gp15, this.gp16));

    // board of game1
    board1 = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(this.col0, this.col1, 
        this.col2, this.col3));

    // board of game 3
    board2 = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(
        new ArrayList<GamePiece>(Arrays.asList(gp01, gp02)), 
        new ArrayList<GamePiece>(Arrays.asList(gp03, gp04))));

    // board of game4
    board3 = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(
        new ArrayList<GamePiece>(Arrays.asList(gpa, gpb)), 
        new ArrayList<GamePiece>(Arrays.asList(gpc, gpd))));

    // makes a 4 by 4 board 
    game1 = new LightEmAll(this.board1);
    // board for playing
    game2 = new LightEmAll(8, 5);
    // makes a 2 by 2 board 
    game3 = new LightEmAll(this.board2, r2);
    // makes a completed un-shuffled 2 by 2 board
    game4 = new LightEmAll(board3);



    // edges in game1
    e1 = new Edge(gp1, gp2, 5);
    e2 = new Edge(gp2, gp3, 3);
    e3 = new Edge(gp3, gp4, 4);
    e4 = new Edge(gp1, gp5, 1);
    e5 = new Edge(gp5, gp6, 2);

    // edges in game3
    e01 = new Edge(gp01, gp02, -341819261);
    e02 = new Edge(gp03, gp04, -489386680);
    e03 = new Edge(gp02, gp04, -1153071262);
    e04 = new Edge(gp01, gp03, 1986100430);

    // list of edges in game1
    edges1 = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4, e5));
    // list of edges in game3
    edges2 = new ArrayList<Edge>(Arrays.asList(e03, e02, e01));
    // list of all possible edges in game3 (unsorted)
    edges3 = new ArrayList<Edge>(Arrays.asList(e04, e01, e03, e02));
    // list of all possible edges in game3 (sorted)
    edges4 = new ArrayList<Edge>(Arrays.asList(e03, e02, e01, e04));
    // list of all possible edges in game3 (sorted)
    edges5 = new ArrayList<Edge>(Arrays.asList(
        new Edge(gpa, gpc, 5), new Edge(gpb, gpd, 7), new Edge(gpc, gpd, 11)));

    // hashMap examples
    hm1 = new HashMap<GamePiece, GamePiece>();

    // adds the edges to game4
    this.game4.mst = edges5;
  }

  // test sortEdges
  boolean testSortEdges(Tester t) {
    reset();
    // should return a list of edges sorted by increasing weight 
    return t.checkExpect(new Utils().sortEdges(this.edges1), new ArrayList<Edge>(Arrays.asList(e4, 
        e5, e2, e3, e1)))
        && t.checkExpect(new Utils().sortEdges(this.edges3), this.edges4);
  }

  // test listNode
  boolean testListNode(Tester t) {
    reset();
    return t.checkExpect(new Utils().listNode(this.board1), this.node1);
  }

  // test powerLocation
  boolean testPowerLocation(Tester t) {
    reset();
    return t.checkExpect(new Utils().powerLocation("row", board1), 2)
        && t.checkExpect(new Utils().powerLocation("col", board1), 2);
  }

  // test checkPieces
  boolean testCheckPieces(Tester t) {
    reset(); 
    return t.checkExpect(this.game1.checkPieces("up", gp1), false) 
        && t.checkExpect(this.game1.checkPieces("down", gp2), true) 
        && t.checkExpect(this.game1.checkPieces("down", gp16), false) 
        && t.checkExpect(this.game1.checkPieces("down", gp16), false) 
        && t.checkExpect(this.game1.checkPieces("right", gp16), false) 
        && t.checkExpect(this.game1.checkPieces("up", gp4), true) 
        && t.checkExpect(this.game1.checkPieces("left", gp4), false) 
        && t.checkExpect(this.game1.checkPieces("left", gp13), false)
        && t.checkExpect(this.game1.checkPieces("down", gp13), true)
        && t.checkExpect(this.game1.checkPieces("right", gp13), false)
        && t.checkExpect(this.game1.checkPieces("right", gp11), true)
        && t.checkExpect(this.game1.checkPieces("up", gp11), true)
        && t.checkExpect(this.game1.checkPieces("down", gp11), true)
        && t.checkExpect(this.game1.checkPieces("left", gp11), true);
  }

  //test bfs
  void testbfs(Tester t) {
    reset();
    // unshuffled game board
    // GamePieces before bfs -> only powerStation should be powered (gp11)
    t.checkExpect(this.gp1.powered, false);
    t.checkExpect(this.gp3.powered, false);
    t.checkExpect(this.gp8.powered, false);
    t.checkExpect(this.gp10.powered, false);
    t.checkExpect(this.gp11.powered, true);
    t.checkExpect(this.gp16.powered, false);
    this.game1.bfs();
    // all connected to powerStation -> all pieces should be powered
    t.checkExpect(this.gp1.powered, true);
    t.checkExpect(this.gp3.powered, true);
    t.checkExpect(this.gp8.powered, true);
    t.checkExpect(this.gp10.powered, true);
    t.checkExpect(this.gp11.powered, true);
    t.checkExpect(this.gp16.powered, true);
    reset();
    // shuffled with seeded random -> only connected pieces should be powered
    this.game1.shuffle(r);
    this.game1.bfs();
    t.checkExpect(this.gp1.powered, false);
    t.checkExpect(this.gp3.powered, true);
    t.checkExpect(this.gp8.powered, false);
    t.checkExpect(this.gp10.powered, true);
    t.checkExpect(this.gp11.powered, true);
    t.checkExpect(this.gp16.powered, false);
  }

  // to test rotate 
  void testRotate(Tester t) {
    reset();
    // tests a top piece with only a bottom opening
    t.checkExpect(this.gp1.bottom, true);
    this.game1.rotate(this.gp1);
    t.checkExpect(this.gp1.bottom, false);
    t.checkExpect(this.gp1.left, true);
    // tests a middle piece with a top and bot opening
    t.checkExpect(this.gp2.bottom, true);
    t.checkExpect(this.gp2.top, true);
    this.game1.rotate(this.gp2);
    t.checkExpect(this.gp2.bottom, false);
    t.checkExpect(this.gp2.top, false);
    t.checkExpect(this.gp2.left, true);
    t.checkExpect(this.gp2.right, true);
    // tests a piece with three openings
    t.checkExpect(this.gp3.bottom, true);
    t.checkExpect(this.gp3.top, true);
    t.checkExpect(this.gp3.right, true);
    this.game1.rotate(this.gp3);
    t.checkExpect(this.gp3.bottom, true);
    t.checkExpect(this.gp2.top, false);
    t.checkExpect(this.gp3.left, true);
    t.checkExpect(this.gp3.right, true);
    // tests a piece with 4 openings 
    t.checkExpect(this.gp7.bottom, true);
    t.checkExpect(this.gp7.top, true);
    t.checkExpect(this.gp7.right, true);
    t.checkExpect(this.gp7.left, true);
    this.game1.rotate(this.gp3);
    t.checkExpect(this.gp7.bottom, true);
    t.checkExpect(this.gp7.top, true);
    t.checkExpect(this.gp7.left, true);
    t.checkExpect(this.gp7.right, true);
  }

  //to test shuffle
  void testShuffle(Tester t) {
    reset();
    t.checkExpect(this.gp1.bottom, true);
    t.checkExpect(this.gp2.bottom, true);
    t.checkExpect(this.gp2.top, true);
    t.checkExpect(this.gp3.bottom, true);
    t.checkExpect(this.gp3.right, true);
    t.checkExpect(this.gp7.right, true);
    t.checkExpect(this.gp16.top, true);
    t.checkExpect(this.gp15.left, true);
    t.checkExpect(this.gp15.bottom, true);
    this.game1.shuffle(r);
    t.checkExpect(this.gp1.bottom, false);
    t.checkExpect(this.gp2.bottom, false);
    t.checkExpect(this.gp2.right, true);
    t.checkExpect(this.gp3.bottom, true);
    t.checkExpect(this.gp3.right, true);
    t.checkExpect(this.gp7.right, true);
    t.checkExpect(this.gp16.top, false);
    t.checkExpect(this.gp15.left, true);
    t.checkExpect(this.gp15.bottom, false);
  }

  //to test Connected 
  boolean testConnected(Tester t) {
    reset();
    // to test two tiles up and down from each other
    return t.checkExpect(this.gp1.connected(this.gp2), true)
        // to test two tiles next to each other 
        && t.checkExpect(this.gp3.connected(this.gp7), true)
        // to test two pieces diagonal 
        && t.checkExpect(this.gp6.connected(this.gp11), false)
        // to test to pieces not near each other 
        && t.checkExpect(this.gp11.connected(this.gp16), false);
  }

  // test onMouseClicked
  void testOnMouseClicked(Tester t) {
    reset();
    t.checkExpect(gp1.top, false);
    t.checkExpect(gp1.bottom, true);
    t.checkExpect(gp1.left, false);
    t.checkExpect(gp1.right, false);
    this.game1.onMouseClicked(new Posn(20, 20));
    t.checkExpect(gp1.top, false);
    t.checkExpect(gp1.bottom, false);
    t.checkExpect(gp1.left, true);
    t.checkExpect(gp1.right, false);

    t.checkExpect(gp2.top, true);
    t.checkExpect(gp2.bottom, true);
    t.checkExpect(gp2.left, false);
    t.checkExpect(gp2.right, false);
    this.game1.onMouseClicked(new Posn(20, 80));
    t.checkExpect(gp2.top, false);
    t.checkExpect(gp2.bottom, false);
    t.checkExpect(gp2.left, true);
    t.checkExpect(gp2.right, true);

    t.checkExpect(gp15.top, true);
    t.checkExpect(gp15.bottom, true);
    t.checkExpect(gp15.left, true);
    t.checkExpect(gp15.right, false);
    this.game1.onMouseClicked(new Posn(200, 150));
    t.checkExpect(gp15.top, true);
    t.checkExpect(gp15.bottom, false);
    t.checkExpect(gp15.left, true);
    t.checkExpect(gp15.right, true);
  }

  // test onKeyEvent
  void testOnKeyEvent(Tester t) {
    reset();
    t.checkExpect(game1.powerCol, 2);
    t.checkExpect(game1.powerRow, 2);
    t.checkExpect(this.gp11.powerStation, true);
    this.game1.onKeyEvent("up");
    t.checkExpect(game1.powerCol, 2);
    t.checkExpect(game1.powerRow, 1);
    t.checkExpect(this.gp10.powerStation, true);
    this.game1.onKeyEvent("down");
    t.checkExpect(game1.powerCol, 2);
    t.checkExpect(game1.powerRow, 2);
    t.checkExpect(this.gp11.powerStation, true);
    this.game1.onKeyEvent("right");
    t.checkExpect(game1.powerCol, 3);
    t.checkExpect(game1.powerRow, 2);
    t.checkExpect(this.gp15.powerStation, true);
    // does not change if there is no connected piece in given direction
    this.game1.onKeyEvent("right");
    t.checkExpect(game1.powerCol, 3);
    t.checkExpect(game1.powerRow, 2);
    t.checkExpect(this.gp15.powerStation, true);
    this.game1.onKeyEvent("left");
    t.checkExpect(game1.powerCol, 2);
    t.checkExpect(game1.powerRow, 2);
    t.checkExpect(this.gp11.powerStation, true);
  }

  //to test makeBoard 
  boolean testMakeBoard(Tester t) {
    reset();
    // to test valid board sizes 
    return t.checkExpect(new Utils().makeBoard(4, 4).size(), 4) 
        && t.checkExpect(new Utils().makeBoard(4, 4).get(0).size(), 4)
        && t.checkExpect(new Utils().makeBoard(10, 14).size(), 10) 
        && t.checkExpect(new Utils().makeBoard(10, 14).get(0).size(), 14) 
        // to test invalid board sizes 
        && t.checkException(new IllegalArgumentException("Invalid height or width"),
            new Utils(), "makeBoard", 1, 1)
        && t.checkException(new IllegalArgumentException("Invalid height or width"),
            new Utils(), "makeBoard", 21, 15);
  }

  // test makePieces
  void testMakePieces(Tester t) {
    reset();
    // makes a blank 4x4 board -> no powerstation, no wires
    ArrayList<ArrayList<GamePiece>> board3 = new Utils().makeBoard(4, 4);
    t.checkExpect(board3.get(0).get(0).top, false);
    t.checkExpect(board3.get(0).get(0).bottom, false);
    t.checkExpect(board3.get(0).get(0).left, false);
    t.checkExpect(board3.get(0).get(0).right, false);
    t.checkExpect(board3.get(0).get(0).powerStation, false);

    t.checkExpect(board3.get(0).get(1).top, false);
    t.checkExpect(board3.get(0).get(1).bottom, false);
    t.checkExpect(board3.get(0).get(1).left, false);
    t.checkExpect(board3.get(0).get(1).right, false);
    t.checkExpect(board3.get(0).get(1).powerStation, false);

    t.checkExpect(board3.get(0).get(2).top, false);
    t.checkExpect(board3.get(0).get(2).bottom, false);
    t.checkExpect(board3.get(0).get(2).left, false);
    t.checkExpect(board3.get(0).get(2).right, false);
    t.checkExpect(board3.get(0).get(2).powerStation, false);

    t.checkExpect(board3.get(0).get(3).top, false);
    t.checkExpect(board3.get(0).get(3).bottom, false);
    t.checkExpect(board3.get(0).get(3).left, false);
    t.checkExpect(board3.get(0).get(3).right, false);
    t.checkExpect(board3.get(0).get(3).powerStation, false);

    t.checkExpect(board3.get(2).get(2).top, false);
    t.checkExpect(board3.get(2).get(2).bottom, false);
    t.checkExpect(board3.get(2).get(2).left, false);
    t.checkExpect(board3.get(2).get(2).right, false);
    t.checkExpect(board3.get(2).get(2).powerStation, false);

    // check that pieces are made correctly given position
    // check powerStation is in correct spot
    new Utils().makePieces(board3, 2, 2);

    t.checkExpect(board3.get(0).get(0).top, false);
    t.checkExpect(board3.get(0).get(0).bottom, true);
    t.checkExpect(board3.get(0).get(0).left, false);
    t.checkExpect(board3.get(0).get(0).right, false);
    t.checkExpect(board3.get(0).get(0).powerStation, false);

    t.checkExpect(board3.get(0).get(1).top, true);
    t.checkExpect(board3.get(0).get(1).bottom, true);
    t.checkExpect(board3.get(0).get(1).left, false);
    t.checkExpect(board3.get(0).get(1).right, false);
    t.checkExpect(board3.get(0).get(1).powerStation, false);

    t.checkExpect(board3.get(0).get(2).top, true);
    t.checkExpect(board3.get(0).get(2).bottom, true);
    t.checkExpect(board3.get(0).get(2).left, false);
    t.checkExpect(board3.get(0).get(2).right, true);
    t.checkExpect(board3.get(0).get(2).powerStation, false);

    t.checkExpect(board3.get(0).get(3).top, true);
    t.checkExpect(board3.get(0).get(3).bottom, false);
    t.checkExpect(board3.get(0).get(3).left, false);
    t.checkExpect(board3.get(0).get(3).right, false);
    t.checkExpect(board3.get(0).get(3).powerStation, false);

    t.checkExpect(board3.get(2).get(2).top, true);
    t.checkExpect(board3.get(2).get(2).bottom, true);
    t.checkExpect(board3.get(2).get(2).left, true);
    t.checkExpect(board3.get(2).get(2).right, true);
    t.checkExpect(board3.get(2).get(2).powerStation, true);
  }

  // test gameWon
  void testGameWon(Tester t) {
    reset();
    t.checkExpect(this.game1.gameWon(), false);
    this.game1.bfs();
    t.checkExpect(this.game1.gameWon(), true);
  }

  // tests find 
  boolean testFind(Tester t) {
    reset();
    // set all game piece's representatives to themselves 
    hm1.put(this.gp01, this.gp01);
    hm1.put(this.gp02, this.gp04);
    hm1.put(this.gp03, this.gp01);
    hm1.put(this.gp04, this.gp03);
    // each game piece's representative should be itself
    return t.checkExpect(this.game3.find(this.hm1, this.gp01), this.gp01)
        && t.checkExpect(this.game3.find(this.hm1, this.gp02), this.gp01)
        && t.checkExpect(this.game3.find(this.hm1, this.gp03), this.gp01)
        && t.checkExpect(this.game3.find(this.hm1, this.gp04), this.gp01); 
  }

  // test union 
  void testUnion(Tester t) {
    reset();
    // set all game piece's representatives to themselves 
    hm1.put(this.gp01, this.gp01);
    hm1.put(this.gp02, this.gp02);
    hm1.put(this.gp03, this.gp03);
    hm1.put(this.gp04, this.gp04);
    // check that gp01's representative is itself
    t.checkExpect(this.game3.find(this.hm1, this.gp01), this.gp01);
    // union gp03 and gp01
    this.game3.union(this.hm1, this.gp03, this.gp01);
    // check that gp03's representative is now gp01
    t.checkExpect(this.game3.find(this.hm1, this.gp03), this.gp01);
    // union gp04 and gp03
    this.game3.union(this.hm1, this.gp04, this.gp03);
    // check that gp04's representative is now also gp01
    t.checkExpect(this.game3.find(this.hm1, this.gp04), this.gp01);
    // union gp02 and gp04
    this.game3.union(this.hm1, this.gp02, this.gp04);
    // check that gp02's representative is now also gp01
    t.checkExpect(this.game3.find(this.hm1, this.gp02), this.gp01);
  }

  // test kruskal 
  boolean testKruskal(Tester t) {
    reset();
    // should return a list of edges that are in the game, sorted by increasing weights
    return t.checkExpect(this.game3.kruskal(), this.edges2);
  }

  //test loEdges
  boolean testLoEdges(Tester t) {
    reset();
    // should return a list of all possible edges in a game, unsorted 
    return t.checkExpect(this.game3.loEdges(this.game3.rand), this.edges3);
  }

  // to test boardComplete 
  void testBoardComplete(Tester t) {
    reset();

    t.checkExpect(board3.get(0).get(0).top, false);
    t.checkExpect(board3.get(0).get(0).bottom, false);
    t.checkExpect(board3.get(0).get(0).left, false);
    t.checkExpect(board3.get(0).get(0).right, false);

    t.checkExpect(board3.get(0).get(1).top, false);
    t.checkExpect(board3.get(0).get(1).bottom, false);
    t.checkExpect(board3.get(0).get(1).left, false);
    t.checkExpect(board3.get(0).get(1).right, false);

    t.checkExpect(board3.get(1).get(0).top, false);
    t.checkExpect(board3.get(1).get(0).bottom, false);
    t.checkExpect(board3.get(1).get(0).left, false);
    t.checkExpect(board3.get(1).get(0).right, false);

    t.checkExpect(board3.get(1).get(1).top, false);
    t.checkExpect(board3.get(1).get(1).bottom, false);
    t.checkExpect(board3.get(1).get(1).left, false);
    t.checkExpect(board3.get(1).get(1).right, false);

    // makes a complete board (with the pieces drawn) and shuffles it
    new Utils().boardComplete(this.edges5);

    t.checkExpect(board3.get(0).get(0).top, false);
    t.checkExpect(board3.get(0).get(0).bottom, true);
    t.checkExpect(board3.get(0).get(0).left, false);
    t.checkExpect(board3.get(0).get(0).right, false);

    t.checkExpect(board3.get(0).get(1).top, false);
    t.checkExpect(board3.get(0).get(1).bottom, true);
    t.checkExpect(board3.get(0).get(1).left, false);
    t.checkExpect(board3.get(0).get(1).right, false);

    t.checkExpect(board3.get(1).get(0).top, true);
    t.checkExpect(board3.get(1).get(0).bottom, false);
    t.checkExpect(board3.get(1).get(0).left, false);
    t.checkExpect(board3.get(1).get(0).right, true);

    t.checkExpect(board3.get(1).get(1).top, true);
    t.checkExpect(board3.get(1).get(1).bottom, false);
    t.checkExpect(board3.get(1).get(1).left, true);
    t.checkExpect(board3.get(1).get(1).right, false);
  }

  void testBigBang(Tester t) {
    reset();
    new Utils().boardComplete(edges5);
    LightEmAll world = game2;
    int worldWidth = world.width * 60;
    int worldHeight = world.height * 60 + 100;
    world.bigBang(worldWidth, worldHeight);
  }

}


