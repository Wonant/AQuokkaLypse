# AQuokkaLypse
![Alt text](./assets/shared/AQUOKKALPYSE_LOGO.png?raw=true "Title")

Game Name: "Dreamwalker"

Gameplay Prototype:

Upon launching the game, the goal is for the player to reach the red "dream shard" without depleting their "fear meter," which serves as both a health bar and player action resource bar. If the player's fear meter is completely depleted or if the player falls off the map, the player will lose the level. 

Controls:

W : Jump

A : Move Left

D : Move Right

Space Bar : Harvest (used to defeat and drain fear meter from enemies). 0 cost to use. +3 if player successfully harvested an enemy.

Left Click : Create a teleporter where your mouse is if the cursor is within the red radius shown in debug mode. Making contact with an entrance portal will teleport the player to the exit portal. -2 to create a portal. -1 to take a portal.

Shift : Shoot a projectile that will stun enemies (NOT FULLY IMPLEMENTED). -1 to shoot each projectile.

R : Restart Level

N : Next Level

P : Previous Level

ESC : Quit

B : Debug Mode

Enemy AI Guide:

Curiosity Critter:
![Alt text](./assets/shared/curiositycritter.png?raw=true "Title")

If the player has a high "fear meter," the Curiosity Critter will get scared and run away from the player.

If the player has a low "fear meter," the Curiosity Critter will stand still and continuously track the player's movement.

Walking or standing in the Curiosity Critter's vision will cause the player to take damage. The player has frames of invincibility after taking damage.

