uniform mat4 u_Matrix;
uniform float u_Time;

attribute vec3 a_Position;  
attribute vec3 a_Color;
attribute vec3 a_DirectionVector;
attribute float a_ParticleStartTime;
attribute mat4 a_RotationMatrix;

varying vec3 v_Color;
varying float v_ElapsedTime;


void main()                    
{                                	  	  
    v_Color = a_Color;       
    v_ElapsedTime = u_Time - a_ParticleStartTime;    

	//ought to be * 9.8/2,but,to avoid overtop the screen height,used 1/10 here
    float gravityFactor = v_ElapsedTime * v_ElapsedTime / 2.0;
    
    vec3 currentPosition = a_Position + (a_DirectionVector * v_ElapsedTime);   //s2 - s1 = vt + 1/2gt*t 
    currentPosition.y -= gravityFactor;    
    
    //v_Color =(0.25 - (currentPosition.y/2.0 - 0.5) * (currentPosition.y/2.0 - 0.5)) * v_Color;
    gl_Position = u_Matrix * vec4(currentPosition, 1.0);    
    gl_PointSize = 20.0;    
}       