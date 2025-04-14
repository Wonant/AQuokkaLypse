# AQuokkaLypse
![Alt text](./assets/shared/AQUOKKALPYSE_LOGO.png?raw=true "Title")

Game Name: "Dreamwalker"

Gameplay Prototype:

Upon launching the game, the goal is for the player to reach the red "dream shard" without depleting their "fear meter," which serves as both a health bar and player action resource bar. If the player's fear meter is completely depleted or if the player falls off the map, the player will lose the level. 

Controls:

W : Jump

A : Move Left

D : Move Right

Space Bar : Harvest Dash (used to defeat and drain fear meter from enemies). 0 cost to use. +3 if player successfully harvested an enemy.

Left Click : Teleport to where your mouse is (resticted to a limited radius around the player). -2 fear to teleport.

Shift : Shoot a projectile that will stun enemies (aimed with mouse). -1 to shoot each projectile.

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

