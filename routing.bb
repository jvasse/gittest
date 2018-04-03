#include "block.bbh"
#include "clock.bbh"

/*

 #ifdef LOG_DEBUG
 #include "log.bbh"
 #endif

*/
threaddef  #define NO_LIEN  99

threadvar byte position[2];
threadvar byte tab[2];

threadvar  uint8_t lien;
threadvar  uint8_t nbreWaitedAnswers;
threadvar byte xplusBorder;
threadvar byte yplusBorder;
threadvar uint8_t countspawn;


threaddef  #define MYCHUNKS 12
threadextern Chunk* thisChunk;
threadvar  Chunk myChunks[MYCHUNKS];

 /***************/
 /** functions **/
 byte goMessageHandler(void);
 byte sendBackChunk(PRef p);
 byte backMessageHandler(void);
 byte sendCoordChunk(PRef p);
 byte coordMessageHandler(void);
 byte sendExecOn(PRef p, byte px, byte py, byte donnee, byte fonc);
 byte execOn(void);
 byte Itwork(void);
 byte Spawn(void);
 byte getSpawn(uint8_t donnee,uint8_t t);


/******************************/
 void myMain(void) {

   delayMS(200);
   lien=NO_LIEN;
   nbreWaitedAnswers=0;
   position[0] = 0;
   position[1] = 0;
   xplusBorder = WEST;
   yplusBorder = UP;
   tab[0]= 0;
   tab[1]= 0;
   countspawn=0;

   if (thisNeighborhood.n[DOWN] == VACANT && thisNeighborhood.n[EAST] == VACANT) {

          setColor(RED);
          position[0]=127;
          position[1]=127;

          for (uint8_t p=0; p<6; p++) {
            if (thisNeighborhood.n[p] != VACANT) {
               sendCoordChunk(p);
                 nbreWaitedAnswers++;
             }
         }
     }



 while(1) {
         delayMS(100);
         //if (position[0] == 127 && position[1] == 127 && nbreWaitedAnswers == 0)
          //setColor(YELLOW);


   }
}


/******************/
/**** systeme ****/
/******************/

void userRegistration(void) {
    registerHandler(SYSTEM_MAIN, (GenericHandler)&myMain);
}

void freeMyChunk(void) {
  freeChunk(thisChunk);
}


// find a useable chunk
Chunk* getFreeUserChunk(void) {
    Chunk* c;
    int i;

    for(i=0; i<MYCHUNKS; i++) {
        c = &(myChunks[i]);
        if(!chunkInUse(c)) {
            return c;
        }
    }
    return NULL;
}


/***********************/
/** FONCTION EMISSION**/
/**********************/


//***Distribution des coordonnées au voisin***//
byte sendCoordChunk(PRef p) {
      Chunk *c = getFreeUserChunk();

      if (c != NULL) {
          c->data[0]=position[0];
          c->data[1]=position[1];
          if (p==xplusBorder) {
              c->data[0]++;
          } else if (p==5-xplusBorder) {
              c->data[0]--;
          } else if (p==yplusBorder) {
              c->data[1]++;
          } else if (p==5-yplusBorder) {
              c->data[1]--;
          }
          if (sendMessageToPort(c, p, c->data, 2, coordMessageHandler,
 (GenericHandler)&freeMyChunk) == 0) {
              freeChunk(c);
              return 0;
          }
      }
      return 1;
 }

//***Acknowledge***//
 byte sendBackChunk(PRef p) {
     Chunk *c=getFreeUserChunk();
     c->data[0]=position[0];
     c->data[1]=position[1];

     if (c != NULL) {
         if (sendMessageToPort(c, p, c->data, 2, backMessageHandler, &freeMyChunk) == 0) {
             freeChunk(c);
             return 0;
         }
     }
     return 1;
 }



/**************************/
/** FONCTIONS RECEPTION **/
/*************************/

 byte coordMessageHandler(void) {
      if (thisChunk == NULL) return 0;
      byte sender = faceNum(thisChunk);

      //delayMS(100);


      //***Je reçois des coordonnées identiques aux miennes***//
      if (position[0] == thisChunk->data[0] && position[1] == thisChunk->data[1]){
        sendBackChunk(sender);
        return 1;
      }

      //***Je reçois pour la première fois mes coordonnées***//
      else if (position[0] == 0 && position[1] == 0) {
        position[0] = thisChunk->data[0];
        position[1] = thisChunk->data[1];
        lien = sender;
        setColor(GREEN);
         for (uint8_t p=0; p<6; p++) {
          if (p!=sender && thisNeighborhood.n[p] != VACANT) {
            sendCoordChunk(p);
            nbreWaitedAnswers++;
            }
        }
    }

      //***Je reçois des coordonnées meilleurs que mes coordonnées actuelles***//
      else if (position[1] < thisChunk->data[1] || (position[1] == thisChunk->data[1] && position[0] < thisChunk->data[0])) {


          position[0] = thisChunk->data[0];
          position[1] = thisChunk->data[1];
          lien = sender;
          setColor(BLUE);
          nbreWaitedAnswers=0;
          for (uint8_t p=0; p<6; p++) {
           if (p!=sender && thisNeighborhood.n[p] != VACANT) {

             sendCoordChunk(p);
             nbreWaitedAnswers++;
              }
           }
        }


        //***Je reçois des coordonnées moins bonne que mes coordonnées actuelles***//
        else if (position[0] < thisChunk->data[0] || position[1] < thisChunk->data[1]) {
          sendCoordChunk(sender);
          nbreWaitedAnswers++;
        }

        //delayMS(1000);


      if (nbreWaitedAnswers==0 && lien != NO_LIEN){
          sendBackChunk(lien);
        }
        printf("%d,(%d;%d)\n",(int)getGUID(),position[0],position[1]);

      /*  if (getGUID() == 2 || getGUID() == 5 || getGUID() == 31 || getGUID() == 35)
        printf("%d,(%d;%d)\n",(int)getGUID(),position[0],position[1]);*/

      return 1;
}




 byte backMessageHandler(void) {
   if (thisChunk==NULL) return 0;
   uint8_t sender=faceNum(thisChunk);

   //delayMS(100);

   if ( (sender==xplusBorder && thisChunk->data[0] == (position[0]+1) && thisChunk->data[1] == position[1]) ||
        (sender==5-xplusBorder && thisChunk->data[0] == (position[0]-1) && thisChunk->data[1] == position[1]) ||
        (sender==yplusBorder && thisChunk->data[0] == position[0] && thisChunk->data[1] == (position[1]+1)) ||
        (sender==5-yplusBorder && thisChunk->data[0] == position[0] && thisChunk->data[1] == (position[1]-1)) ) {

   nbreWaitedAnswers--;
   //printf("%d, Reponses = %d, Envoyeur: %d\n",(int)getGUID(), nbreWaitedAnswers, sender);

   if (nbreWaitedAnswers==0 && lien == NO_LIEN){
     setColor(YELLOW);
   }



   if (nbreWaitedAnswers==0 && lien != NO_LIEN) {

       setColor(AQUA);
       sendBackChunk(lien);


       if (thisNeighborhood.n[WEST] == VACANT && thisNeighborhood.n[DOWN] == VACANT ){


       uint8_t width = position[0]-126; // afin de se compter dans le caclul
       sendExecOn(EAST,127,127,width,147);
       //printf("%s : %d  \n","largeur envoyée",width);
       //setColor(ORANGE);
     }


       else if (thisNeighborhood.n[EAST] == VACANT && thisNeighborhood.n[UP] == VACANT ){
         //delayMS(1000);

       uint8_t height = position[1]-126;
       sendExecOn(DOWN,127,127,height,148);
       //printf("%s : %d  \n","Hauteur envoyée",height);
       //setColor(ORANGE);
        }
      }
    }
 //delayMS(1000);
   return 1;
 }

//*** +++ ***//


byte sendExecOn(PRef p, byte px, byte py, byte donnee, byte fonc) {
	Chunk *c = getFreeUserChunk();

    //delayMS(200);

    if (c!=NULL) {

		c->data[0] = px;
		c->data[1] = py;
		c->data[2] = donnee;
		c->data[3] = fonc;


    //printf("%d, (%d, %d, %d, %d)\n", (int)getGUID(), c->data[0], c->data[1], c->data[2], c->data[3]);


		if (sendMessageToPort(c, p, c->data, 4, execOn, (GenericHandler)&freeMyChunk) == 0) {
      //printf("%d, %s\n", (int)getGUID(), "sendExecOn");
			freeChunk(c);
			return 0;
		}
	}
	return 1;
}



	byte execOn(void) {
			if (thisChunk==NULL) return 0;
			uint8_t sender = faceNum(thisChunk);
			uint8_t receiver[2];
			receiver[0] = thisChunk->data[0];
			receiver[1] = thisChunk->data[1];
			uint8_t donnee = thisChunk->data[2];
			byte fonc = thisChunk->data[3];

      //printf("%d, (%d;%d) \n",(int)getGUID(),position[0],position[1]);

      //setColor(RED);
      //delayMS(300);

			//c->data[0] = px;
			//c->data[1] = py;
			//c->data[2] = donnee;
			//c->data[3] = fonc;

			if (position[0] != receiver[0]){

				if (position[0] < receiver[0])
					sendExecOn(WEST, receiver[0], receiver[1], donnee, fonc);

				else
					sendExecOn(EAST, receiver[0], receiver[1], donnee, fonc);
			}


			else if (position[1] != receiver[1]){

				if (position[1] < receiver[1])
					sendExecOn(UP, receiver[0], receiver[1], donnee, fonc);

				else
					sendExecOn(DOWN, receiver[0], receiver[1], donnee, fonc);
			}

			else if (position[0] == receiver[0] && position[1] == receiver[1]){
				if (fonc == 147)
          getSpawn(donnee,1);
        if (fonc == 148)
          getSpawn(donnee,2);

      if (fonc == 166)
        Spawn();
}

			return 1;
}

byte getSpawn(uint8_t donnee, uint8_t t){

//printf("%s : %d \n","execution de getspawn avc data : ", donnee );

  if (t == 1){
    tab[0] = (donnee/2);
    countspawn++;
    //printf("%s : %d  \n","COORD X RECUES : " ,127+tab[0]);
  }
  else if (t == 2){
    tab[1] = donnee;
    countspawn++;
    //printf("%s : %d \n","COORD Y RECUES : " ,127+tab[1]);
  }



    if (countspawn == 2){
    //delayMS(200);
      sendExecOn(WEST,127+tab[0],126+tab[1],0,166);
      return 0;
  }
  return 1;

}


byte Spawn(void){
//while (1) {
    setColor(WHITE);
////    setColor(PURPLE);
//    delayMS(150);

/* THE GAME BEGINS */
//}


  return 0;
}
