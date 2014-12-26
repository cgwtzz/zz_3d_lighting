/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.particles.android;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_LEQUAL;
import static android.opengl.GLES20.GL_LESS;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDepthFunc;
import static android.opengl.GLES20.glDepthMask;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.invertM;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.multiplyMV;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.scaleM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;
import static android.opengl.Matrix.transposeM;

import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.Log;

import com.particles.android.objects.Heightmap;
import com.particles.android.objects.ParticleShooter;
import com.particles.android.objects.ParticleSystem;
import com.particles.android.objects.Skybox;
import com.particles.android.programs.HeightmapShaderProgram;
import com.particles.android.programs.ParticleShaderProgram;
import com.particles.android.programs.SkyboxShaderProgram;
import com.particles.android.util.Geometry.Point;
import com.particles.android.util.Geometry.Vector;
import com.particles.android.util.MatrixHelper;
import com.particles.android.util.TextureHelper;

public class ParticlesRenderer implements Renderer {    
    private final Context context;

    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewMatrixForSkybox = new float[16];
    private final float[] projectionMatrix = new float[16];        
    
    private final float[] tempMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] it_modelViewMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];

    private HeightmapShaderProgram heightmapProgram;
    private Heightmap heightmap;
    
    /*
    private final Vector vectorToLight = new Vector(0.61f, 0.64f, -0.47f).normalize();
    */ 
    /*
    private final Vector vectorToLight = new Vector(0.30f, 0.35f, -0.89f).normalize();
    */
    final float[] vectorToLight = {0.30f, 0.35f, -0.89f, 0f};
    
    private final float[] pointLightPositions = new float[]
        {-1f, 1f, 0f, 1f,
          0f, 1f, 0f, 1f,
          1f, 1f, 0f, 1f};
    
    private final float[] pointLightColors = new float[]
        {1.00f, 0.20f, 0.02f,
         0.02f, 0.25f, 0.02f, 
         0.02f, 0.20f, 1.00f};
    
    private SkyboxShaderProgram skyboxProgram;
    private Skybox skybox;   
    
    private ParticleShaderProgram particleProgram;      
    private ParticleSystem particleSystem;
    private ParticleShooter redParticleShooter;
    private ParticleShooter greenParticleShooter;
    private ParticleShooter blueParticleShooter;     

    private long globalStartTime; //ns
    private float lastOndrawFrameTime; //s
    private float frameUsedTime; //s
    private int fps;
    private int firstOnDrawFrame;
    
    private int particleTexture;
    private int skyboxTexture;
    
    private float xRotation, yRotation;  

    public ParticlesRenderer(Context context) {
        this.context = context;
    }

    public void handleTouchDrag(float deltaX, float deltaY) {
        xRotation += deltaX / 16f;
        yRotation += deltaY / 16f;
        
        if (yRotation < -90) {
            yRotation = -90;
        } else if (yRotation > 90) {
            yRotation = 90;
        } 
        
        // Setup view matrix
        updateViewMatrices();        
    }
    public void handleAutoRotation(float rotateAngle) {
        xRotation += rotateAngle;
        
        if (xRotation < -360) {
            xRotation = -xRotation % 360;
        } else if (xRotation > 360) {
            xRotation = xRotation % 360;
        } 
        
        // Setup view matrix
        rotateViewMatrices(xRotation);        
    }
    private void rotateViewMatrices(float xRotation ) {      //instead of updateViewMatrices in auto rotate operation
        setIdentityM(viewMatrix, 0);
        //rotateM(viewMatrix, 0, -yRotation, 1f, 0f, 0f);
        rotateM(viewMatrix, 0, -xRotation, 0f, 1f, 0f);
        System.arraycopy(viewMatrix, 0, viewMatrixForSkybox, 0, viewMatrix.length);
        
        // We want the translation to apply to the regular view matrix, and not
        // the skybox.
        translateM(viewMatrix, 0, 0, -1.5f, -5f);
    }
    
    private void updateViewMatrices() {        
        setIdentityM(viewMatrix, 0);
        rotateM(viewMatrix, 0, -yRotation, 1f, 0f, 0f);
        rotateM(viewMatrix, 0, -xRotation, 0f, 1f, 0f);
        System.arraycopy(viewMatrix, 0, viewMatrixForSkybox, 0, viewMatrix.length);
        
        // We want the translation to apply to the regular view matrix, and not
        // the skybox.
        translateM(viewMatrix, 0, 0, -1.5f, -5f);

        // This helps us figure out the vector for the sun or the moon.        
        final float[] tempVec = {0f, 0f, -1f, 1f};
        final float[] tempVec2 = new float[4];
        
        Matrix.multiplyMV(tempVec2, 0, viewMatrixForSkybox, 0, tempVec, 0);
        Log.i("ParticleSytem", Arrays.toString(tempVec2));
    }  

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);  
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        
        heightmapProgram = new HeightmapShaderProgram(context);
        heightmap = new Heightmap(((BitmapDrawable)context.getResources()
            .getDrawable(R.drawable.heightmap)).getBitmap());
        
        skyboxProgram = new SkyboxShaderProgram(context);
        skybox = new Skybox();      
        
        particleProgram = new ParticleShaderProgram(context);        
        particleSystem = new ParticleSystem(10000);        
        globalStartTime = System.nanoTime();
        lastOndrawFrameTime = globalStartTime / 1000000000f;
        frameUsedTime = 0;
        fps = 0;
        firstOnDrawFrame = 1;
        
        final Vector particleDirection = new Vector(0f, 0.5f, 0f);              
        final float angleVarianceInDegrees = 5f; 
        final float speedVariance = 1f;
            
        redParticleShooter = new ParticleShooter(
            new Point(-1f, 0f, 0f), 
            particleDirection,                
            Color.rgb(255, 50, 5),            
            angleVarianceInDegrees, 
            speedVariance);
        
        greenParticleShooter = new ParticleShooter(
            new Point(0f, 0f, 0f), 
            particleDirection,
            Color.rgb(25, 255, 25),            
            angleVarianceInDegrees, 
            speedVariance);
        
        blueParticleShooter = new ParticleShooter(
            new Point(1f, 0f, 0f), 
            particleDirection,
            Color.rgb(5, 50, 255),            
            angleVarianceInDegrees, 
            speedVariance); 
                
        particleTexture = TextureHelper.loadTexture(context, R.drawable.particle_texture);
        
//        skyboxTexture = TextureHelper.loadCubeMap(context, 
//            new int[] { R.drawable.left, R.drawable.right,
//                        R.drawable.bottom, R.drawable.top, 
//                        R.drawable.front, R.drawable.back}); 
        skyboxTexture = TextureHelper.loadCubeMap(context, 
        new int[] { R.drawable.night_left, R.drawable.night_right,
                    R.drawable.night_bottom, R.drawable.night_top, 
                    R.drawable.night_front, R.drawable.night_back});
        
        
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {                
        glViewport(0, 0, width, height);        

        MatrixHelper.perspectiveM(projectionMatrix, 45, (float) width
            / (float) height, 1f, 100f);   
        updateViewMatrices();
    }

    @Override    
    public void onDrawFrame(GL10 glUnused) {
        float thisdrawStartTime = (System.nanoTime() - globalStartTime) / 1000000000f;
        lastOndrawFrameTime = thisdrawStartTime;
        
        float toLastFrmeTime = thisdrawStartTime - lastOndrawFrameTime;
        if(firstOnDrawFrame == 1){
            toLastFrmeTime = 0; 
        }
        firstOnDrawFrame = 0;
        frameUsedTime += toLastFrmeTime;
        if(frameUsedTime > 60){
            frameUsedTime = 0;
            Log.i("GLFramePerSecond","fps:" + fps );
            fps = 0;
        }
        else if((frameUsedTime > 0) && (frameUsedTime <= 60)){
            fps += 1;
            Log.i("GLFramePerSecond","toLastFrmeTime:" + toLastFrmeTime );
        }
        else {
            Log.w("GLFramePerSecond","toLastFrmeTime is negative number:" + toLastFrmeTime );
        }
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);  
        
        handleAutoRotation(6);// 6 * 60 = 360 degree.
        
        drawHeightmap();
        drawSkybox();        
        drawParticles();
        
        float thisdrawEndTime = (System.nanoTime() - globalStartTime) / 1000000000f;
        float drawUsedTime = thisdrawEndTime - thisdrawStartTime;
        Log.i("GLFrameDraw","drawUsedTime:" + drawUsedTime );
        
    }

    private void drawHeightmap() {
        float thisHeightMapStartTime = (System.nanoTime() - globalStartTime) / 1000000000f;
        setIdentityM(modelMatrix, 0);  
        
        // Expand the heightmap's dimensions, but don't expand the height as
        // much so that we don't get insanely tall mountains.        
        scaleM(modelMatrix, 0, 100f, 10f, 100f);
        updateMvpMatrix();        
        
        heightmapProgram.useProgram();
        /*
        heightmapProgram.setUniforms(modelViewProjectionMatrix, vectorToLight);
         */
        
        // Put the light positions into eye space.        
        final float[] vectorToLightInEyeSpace = new float[4];
        final float[] pointPositionsInEyeSpace = new float[12];                
        multiplyMV(vectorToLightInEyeSpace, 0, viewMatrix, 0, vectorToLight, 0);
        multiplyMV(pointPositionsInEyeSpace, 0, viewMatrix, 0, pointLightPositions, 0);
        multiplyMV(pointPositionsInEyeSpace, 4, viewMatrix, 0, pointLightPositions, 4);
        multiplyMV(pointPositionsInEyeSpace, 8, viewMatrix, 0, pointLightPositions, 8); 
        
        heightmapProgram.setUniforms(modelViewMatrix, it_modelViewMatrix, 
            modelViewProjectionMatrix, vectorToLightInEyeSpace,
            pointPositionsInEyeSpace, pointLightColors);
        heightmap.bindData(heightmapProgram);
        heightmap.draw();
        float thisHeightMapEndTime = (System.nanoTime() - globalStartTime) / 1000000000f;
        Log.i("drawHeightmap","drawHeightmapUsedTime:" + (thisHeightMapEndTime - thisHeightMapStartTime ) );
    }
    
    private void drawSkybox() {   
        float thisSkyboxStartTime = (System.nanoTime() - globalStartTime) / 1000000000f;
        setIdentityM(modelMatrix, 0);
        updateMvpMatrixForSkybox();
        //rotateM();
                
        glDepthFunc(GL_LEQUAL); // This avoids problems with the skybox itself getting clipped.
        skyboxProgram.useProgram();
        skyboxProgram.setUniforms(modelViewProjectionMatrix, skyboxTexture);
        skybox.bindData(skyboxProgram);
        skybox.draw();
        glDepthFunc(GL_LESS);
        
        
        float thisSkyboxEndTime = (System.nanoTime() - globalStartTime) / 1000000000f;
        Log.i("drawSkybox","drawSkyboxUsedTime:" + (thisSkyboxEndTime - thisSkyboxStartTime ) );
    }
   
    private void drawParticles() {        
        float currentTime = (System.nanoTime() - globalStartTime) / 1000000000f;
        float thisParticlesStartTime = currentTime;
        redParticleShooter.addParticles(particleSystem, currentTime, 1);
        greenParticleShooter.addParticles(particleSystem, currentTime, 1);              
        blueParticleShooter.addParticles(particleSystem, currentTime, 1);              
        
        setIdentityM(modelMatrix, 0);
        updateMvpMatrix();
        
        glDepthMask(false);
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE);
        
        particleProgram.useProgram();
        particleProgram.setUniforms(modelViewProjectionMatrix, currentTime, particleTexture);
        particleSystem.bindData(particleProgram);
        
     // Allocate a buffer.
        final float drawTime[] = new float[1];
        particleSystem.draw(drawTime); 
        Log.i("ParticleSytemDrawTime", "" + Float.toString(drawTime[0]));
        
        glDisable(GL_BLEND);
        glDepthMask(true);
        float thisParticlesEndTime = (System.nanoTime() - globalStartTime) / 1000000000f;
        Log.i("drawParticles","drawParticlesUsedTime:" + (thisParticlesEndTime - thisParticlesStartTime ) );
    }
    
    /*
    private void updateMvpMatrix() {
        multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);        
        multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
    }
    */
    
    private void updateMvpMatrix() {
        multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        invertM(tempMatrix, 0, modelViewMatrix, 0);
        transposeM(it_modelViewMatrix, 0, tempMatrix, 0);        
        multiplyMM(
            modelViewProjectionMatrix, 0, 
            projectionMatrix, 0, 
            modelViewMatrix, 0);
    }
        
    private void updateMvpMatrixForSkybox() {
        multiplyMM(tempMatrix, 0, viewMatrixForSkybox, 0, modelMatrix, 0);
        multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
    }
}