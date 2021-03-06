/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.particles.android.objects;

import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;
import static android.opengl.Matrix.rotateM;

import java.util.Random;

import android.graphics.Color;
import android.opengl.Matrix;

import com.particles.android.util.Geometry.Point;
import com.particles.android.util.Geometry.Vector;

/** This class shoots particles in a particular direction. */

public class ParticleShooter {       
    public final Point position;    
    public  int color;    

    private final float angleVariance;
    private final float speedVariance;    
    
    private final Random random = new Random();
    
    private float[] ParticleMatrix = new float[16];
    private float[] directionVector = new float[4];
    private float[] resultVector = new float[4];

    public ParticleShooter(
        Point position, Vector direction, int color, 
        float angleVarianceInDegrees, float speedVariance) {

        this.position = position;        
        this.color = color;        

        this.angleVariance = angleVarianceInDegrees;
        this.speedVariance = speedVariance;        
        
        directionVector[0] = direction.x;
        directionVector[1] = direction.y;
        directionVector[2] = direction.z;        
    }

    public void addParticles(ParticleSystem particleSystem, float currentTime, 
        int count,float start_angle,float angle_range) {  
        
        float angle_incre = angle_range/count;
        float angle = start_angle;
        
        float sameSpeedRandom = random.nextFloat();
        float speedAdjustment = 1.0f;
        float directionFactor = 1.0f;
        int randomred = random.nextInt(255);
        int randomgreen = random.nextInt(255);
        int randomblue = random.nextInt(255);
        
        for (int i = 0; i < count; i++) {  
            angle += angle_incre * i;
            setIdentityM(ParticleMatrix, 0);    
            rotateM(ParticleMatrix, 0, -angle, 0f, 1f, 0f);
            /*
            Matrix.setRotateEulerM(rotationMatrix, 0, 
                (random.nextFloat() - 0.5f) * angleVariance,//can not fire to -y direction 
                (random.nextFloat() - 0.5f) * angleVariance, 
                (random.nextFloat() - 0.5f) * angleVariance);
            */
            
            /*
            Matrix.multiplyMV(
                resultVector, 0, 
                rotationMatrix, 0, 
                directionVector, 0);
                */
            
            //normalize resultVerctor.
            
            //float rls = 1.0f / Matrix.length(resultVector[0], resultVector[1], resultVector[2]);
            if(sameSpeedRandom > 0.4f){
                                
                 if(sameSpeedRandom > 0.6 ){
                     //color = Color.rgb(randomred, randomgreen, randomblue)/15;
                     //speedAdjustment = 1.0f + random.nextFloat() * speedVariance * rls;
                    // directionFactor = 0.22f;
                     color = Color.rgb(255, 50, 5);
                     
                      }
                 else{
                     //speedAdjustment = 0.8f + random.nextFloat() * speedVariance * rls;
                     //color = 0xFF000000;//black
                     //directionFactor = 0.8f;
                     color = Color.rgb(5, 50, 250);
                     
                     }
            }else{
                color = Color.rgb(0, 0, 0);
                //speedAdjustment = random.nextFloat()  *speedVariance * rls + 1.2f ;
                //speedAdjustment = 1.5f;
                
            }
            
            
            speedAdjustment = 1.6f;
            directionFactor = 0.55f;
            
            
            Matrix.multiplyMV(
                resultVector, 0, 
                ParticleMatrix, 0, 
                directionVector, 0);
            
           
            
            //speedAdjustment = 1.8f;
            Vector thisDirection = new Vector(
                resultVector[0] * speedAdjustment * directionFactor,
                resultVector[1] * speedAdjustment,
                resultVector[2] * speedAdjustment);        
            
            
             
            particleSystem.addParticle(position, color, thisDirection, currentTime);
        }       
    }
}
