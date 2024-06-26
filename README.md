Game idea: Multiplayer minesweeper 

• Game description:
- Goal: The aim of the game is to find more than half of the possible mines 
before your opponent.
- Equipment: Each player has their own window with a 5x8 board divided into 
40 fields, under which 8 mines are hidden. The state of the board changes 
with every move. The window also shows the players' current score and 
indicates which player you are.
- Gameplay:
   1. The game takes place in turns, with each player making one move in 
    succession.
  2. At the beginning, players have a board with covered fields. In each 
  move, the player reveals one square, which can be either a mine or a 
  number. When a player hits a mine, a point is added to their score. On 
  the other hand, selecting a field with a number allows the player to 
  determine how many mines are in the immediate neighborhood of a 
  given cell and use this knowledge in the next move.
  3. The game ends when one of the players finds more than half of the 
  hidden mines, i.e. their score is equal to 5 or there is a draw.

Example: https://minesweeper-multiplayer.dk/ 

Technologies: 
1. Java   
2. Swing (GUI)   
3. Java.net package   
4. Java.io package


