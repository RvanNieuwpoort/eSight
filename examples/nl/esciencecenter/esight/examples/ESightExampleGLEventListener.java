package nl.esciencecenter.esight.examples;

import java.io.File;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;

import nl.esciencecenter.esight.ESightGLEventListener;
import nl.esciencecenter.esight.datastructures.FBO;
import nl.esciencecenter.esight.datastructures.IntPBO;
import nl.esciencecenter.esight.exceptions.UninitializedException;
import nl.esciencecenter.esight.input.InputHandler;
import nl.esciencecenter.esight.math.MatF4;
import nl.esciencecenter.esight.math.MatrixFMath;
import nl.esciencecenter.esight.math.Point4;
import nl.esciencecenter.esight.math.VecF3;
import nl.esciencecenter.esight.math.VecF4;
import nl.esciencecenter.esight.models.Axis;
import nl.esciencecenter.esight.models.Model;
import nl.esciencecenter.esight.models.Quad;
import nl.esciencecenter.esight.shaders.ShaderProgram;

/* Copyright [2013] [Netherlands eScience Center]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Example implementation of a ESightGLEventListener. Renders Axes in different
 * colors to a texture and renders then this texture to the screen.
 * 
 * @author Maarten van Meersbergen <m.van.meersbergen@esciencecenter.nl>
 * 
 */
/* Copyright [2013] [Netherlands eScience Center]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Maarten van Meersbergen <m.van.meersbergen@esciencecenter.nl>
 * 
 */
public class ESightExampleGLEventListener extends ESightGLEventListener {
    // Two example shader program definitions.
    private ShaderProgram axesShaderProgram, postprocessShader;

    // An example framebuffer object for rendering to textures.
    private FBO axesFBO;

    // Model definitions, the quad is necessary for Full-screen rendering. The
    // axes are the model we wish to render (example)
    private Quad FSQ_postprocess;
    private Model xAxis, yAxis, zAxis;

    // Global (singleton) settings instance.
    private final ESightExampleSettings settings = ESightExampleSettings
            .getInstance();

    // Pixelbuffer Object, we use this to get screenshots.
    private IntPBO finalPBO;

    // Global (singleton) inputhandler instance.
    private final ESightExampleInputHandler inputHandler = ESightExampleInputHandler
            .getInstance();

    // State keeping variable
    private boolean screenshotWanted;

    // Height and width of the drawable area. We extract this from the opengl
    // instance in the reshape method every time it is changed, but set it in
    // the init method initially. The default values are defined by the settings
    // class.
    private int canvasWidth, canvasHeight;

    // Variables needed to calculate the viewpoint and camera angle.
    final Point4 eye = new Point4(
            (float) (radius * Math.sin(ftheta) * Math.cos(phi)),
            (float) (radius * Math.sin(ftheta) * Math.sin(phi)),
            (float) (radius * Math.cos(ftheta)), 1.0f);
    final Point4 at = new Point4(0.0f, 0.0f, 0.0f, 1.0f);
    final VecF4 up = new VecF4(0.0f, 1.0f, 0.0f, 0.0f);

    /**
     * Basic constructor for ESightExampleGLEventListener.
     */
    public ESightExampleGLEventListener() {
        super();

    }

    // Initialization method, this is called by the animator before anything
    // else, and is therefore the perfect place to initialize all of the
    // ShaderPrograms, FrameBuffer objects and such.
    @Override
    public void init(GLAutoDrawable drawable) {
        // Get the Opengl context from the drawable, and make it current, so
        // we can see it and draw on it. I've never seen this fail, but there is
        // error checking anyway.
        try {
            final int status = drawable.getContext().makeCurrent();
            if ((status != GLContext.CONTEXT_CURRENT)
                    && (status != GLContext.CONTEXT_CURRENT_NEW)) {
                System.err.println("Error swapping context to onscreen.");
            }
        } catch (final GLException e) {
            System.err.println("Exception while swapping context to onscreen.");
            e.printStackTrace();
        }

        // Once we have the context current, we can extract the OpenGL instance
        // from it. We have defined a OpenGL 3.0 instance in the
        // ESightNewtWindow by adding the line
        // glp = GLProfile.get(GLProfile.GL3);
        // Therefore, we extract a GL3 instance, so we cannot make any
        // unfortunate mistakes (calls to methods that are undefined for this
        // version).
        final GL3 gl = GLContext.getCurrentGL().getGL3();

        // set the canvas size and aspect ratio in the global variables.
        canvasWidth = GLContext.getCurrent().getGLDrawable().getWidth();
        canvasHeight = GLContext.getCurrent().getGLDrawable().getHeight();
        aspect = (float) canvasWidth / (float) canvasHeight;

        // Enable Anti-Aliasing (smoothing of jagged edges on the edges of
        // objects).
        gl.glEnable(GL3.GL_LINE_SMOOTH);
        gl.glHint(GL3.GL_LINE_SMOOTH_HINT, GL3.GL_NICEST);
        gl.glEnable(GL3.GL_POLYGON_SMOOTH);
        gl.glHint(GL3.GL_POLYGON_SMOOTH_HINT, GL3.GL_NICEST);

        // Enable Depth testing (Render only those objects that are not obscured
        // by other objects).
        gl.glEnable(GL3.GL_DEPTH_TEST);
        gl.glDepthFunc(GL3.GL_LEQUAL);
        gl.glClearDepth(1.0f);

        // Enable Culling (render only the camera-facing sides of objects).
        gl.glEnable(GL3.GL_CULL_FACE);
        gl.glCullFace(GL3.GL_BACK);

        // Enable Blending (needed for both Transparency and Anti-Aliasing)
        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL3.GL_BLEND);

        // Enable Vertical Sync
        gl.setSwapInterval(1);

        // Set black background
        gl.glClearColor(0f, 0f, 0f, 0f);

        // Enable programmatic setting of point size, for rendering points (not
        // needed for this example application).
        gl.glEnable(GL3.GL_PROGRAM_POINT_SIZE);

        // Load and compile shaders from source Files (there are other options;
        // check the ShaderProgram Javadoc).
        try {
            // Create the ShaderProgram that we're going to use for the Example
            // Axes. The source code for the VertexShader: shaders/vs_axes.vp,
            // and the source code for the FragmentShader: shaders/fs_axes.fp
            axesShaderProgram = loader.createProgram(gl, "axes", new File(
                    "shaders/vs_axes.vp"), new File("shaders/fs_axes.fp"));

            // Same for the postprocessing shader.
            postprocessShader = loader.createProgram(gl, "postProcess",
                    new File("shaders/vs_postprocess.vp"), new File(
                            "shaders/fs_examplePostprocess.fp"));
        } catch (final Exception e) {
            // If compilation fails, we will output the error message and quit
            // the application.
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // Here we define the Axis models, and initialize them.
        xAxis = new Axis(new VecF3(-1f, 0f, 0f), new VecF3(1f, 0f, 0f), .1f,
                .02f);
        xAxis.init(gl);
        yAxis = new Axis(new VecF3(0f, -1f, 0f), new VecF3(0f, 1f, 0f), .1f,
                .02f);
        yAxis.init(gl);
        zAxis = new Axis(new VecF3(0f, 0f, -1f), new VecF3(0f, 0f, 1f), .1f,
                .02f);
        zAxis.init(gl);

        // Here we define the Full screen quad model (for postprocessing), and
        // initialize it.
        FSQ_postprocess = new Quad(2, 2, new VecF3(0, 0, 0.1f));
        FSQ_postprocess.init(gl);

        // Here we define some intermediate-step full screen textures (which are
        // needed for post processing), done with FrameBufferObjects, so we can
        // render directly to them.
        axesFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE0);
        axesFBO.init(gl);

        // Here we define a PixelBufferObject, which is used for getting
        // screenshots.
        finalPBO = new IntPBO(canvasWidth, canvasHeight);
        finalPBO.init(gl);
    }

    // Display method, this is called by the animator thread to render a single
    // frame. Expect this to be running 60 times a second.
    // The GLAutoDrawable is a JOGL concept that holds the current opengl state.
    @Override
    public void display(GLAutoDrawable drawable) {
        // Get the Opengl context from the drawable, and make it current, so
        // we can see it and draw on it. I've never seen this fail, but there is
        // error checking anyway.
        try {
            final int status = drawable.getContext().makeCurrent();
            if ((status != GLContext.CONTEXT_CURRENT)
                    && (status != GLContext.CONTEXT_CURRENT_NEW)) {
                System.err.println("Error swapping context to onscreen.");
            }
        } catch (final GLException e) {
            System.err.println("Exception while swapping context to onscreen.");
            e.printStackTrace();
        }

        // Once we have the context current, we can extract the OpenGL instance
        // from it. We have defined a OpenGL 3.0 instance in the
        // ESightNewtWindow by adding the line
        // glp = GLProfile.get(GLProfile.GL3);
        // Therefore, we extract a GL3 instance, so we cannot make any
        // unfortunate mistakes (calls to methods that are undefined for this
        // version).
        final GL3 gl = GLContext.getCurrentGL().getGL3();

        // First, we clear the buffer to start with a clean slate to draw on.
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // Construct a modelview matrix out of camera viewpoint and angle.
        MatF4 modelViewMatrix = MatrixFMath.lookAt(eye, at, up);

        // Translate the camera backwards according to the inputhandler's view
        // distance setting.
        modelViewMatrix = modelViewMatrix.mul(MatrixFMath.translate(new VecF3(
                0f, 0f, inputHandler.getViewDist())));

        // Rotate tha camera according to the rotation angles defined in the
        // inputhandler.
        modelViewMatrix = modelViewMatrix.mul(MatrixFMath
                .rotationX(inputHandler.getRotation().get(0)));
        modelViewMatrix = modelViewMatrix.mul(MatrixFMath
                .rotationY(inputHandler.getRotation().get(1)));
        modelViewMatrix = modelViewMatrix.mul(MatrixFMath
                .rotationZ(inputHandler.getRotation().get(2)));

        // Render the scene with these modelview settings. In this case, the end
        // result of this action will be that the AxesFBO has been filled with
        // the right pixels.
        renderScene(gl, modelViewMatrix);

        // Render the FBO's to screen, doing any post-processing actions that
        // might be wanted.
        renderTexturesToScreen(gl, canvasWidth, canvasHeight);

        // Make a screenshot, when wanted. The PBO copies the current
        // framebuffer. We then set the state back because we dont want to make
        // a screenshot 60 times a second.
        if (screenshotWanted) {
            finalPBO.makeScreenshotPNG(gl, settings.getScreenshotFileName());

            screenshotWanted = false;
        }

        // Release the context.
        try {
            drawable.getContext().release();
        } catch (final GLException e) {
            e.printStackTrace();
        }
    }

    private MatF4 makePerspectiveMatrix() {
        return MatrixFMath.perspective(fovy, aspect, zNear, zFar);
    }

    /**
     * Scene rendering method. we can add more things here to render than only
     * axes.
     * 
     * @param gl
     *            The current openGL instance.
     * @param mv
     *            The current modelview matrix.
     */
    private void renderScene(GL3 gl, MatF4 mv) {
        try {
            renderAxes(gl, mv.clone(), axesShaderProgram, axesFBO);
        } catch (final UninitializedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Axes rendering method. This assumes rendering to an {@link FBO}. This is
     * not a necessity, but it allows for post processing.
     * 
     * @param gl
     *            The current openGL instance.
     * @param mv
     *            The current modelview matrix.
     * @param target
     *            The {@link ShaderProgram} to use for rendering.
     * @param target
     *            The target {@link FBO} to render to.
     * @throws UninitializedException
     *             if the shader Program used in this
     */
    private void renderAxes(GL3 gl, MatF4 mv, ShaderProgram program, FBO target)
            throws UninitializedException {
        // Bind the FrameBufferObject so we can start rendering to it
        target.bind(gl);
        // Clear the renderbuffer to start with a clean (black) slate
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

        // Stage the Perspective and Modelview matrixes in the ShaderProgram.
        program.setUniformMatrix("PMatrix", makePerspectiveMatrix());
        program.setUniformMatrix("MVMatrix", mv);

        // Stage the Color vector in the ShaderProgram.
        program.setUniformVector("Color", new VecF4(1f, 0f, 0f, 1f));

        // Load all staged variables into the GPU, check for errors and
        // omissions.
        program.use(gl);
        // Call the model's draw method, this links the model's VertexBuffer to
        // the ShaderProgram and then calls the OpenGL draw method.
        xAxis.draw(gl, program);

        // Do this 2 more times, with different colors and models.
        program.setUniformVector("Color", new VecF4(0f, 1f, 0f, 1f));
        program.use(gl);
        yAxis.draw(gl, program);

        program.setUniformVector("Color", new VecF4(0f, 0f, 1f, 1f));
        program.use(gl);
        zAxis.draw(gl, program);

        // Unbind the FrameBufferObject, making it available for texture
        // extraction.
        target.unBind(gl);
    }

    /**
     * Final image composition and postprocessing method. makes use of the
     * postprocessShader
     * 
     * @param gl
     *            The current openGL instance.
     * @param width
     *            The width of the openGL 'canvas'
     * @param height
     *            The height of the openGL 'canvas'
     */
    private void renderTexturesToScreen(GL3 gl, int width, int height) {
        // Clear the renderbuffer to start with a clean (black) slate
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // Stage a pointer to the Texture picturing the axes (extracted from the
        // FrameBufferObject) in the shaderprogram.
        postprocessShader.setUniform("axesTexture", axesFBO.getTexture()
                .getMultitexNumber());

        // Stage the Perspective and Modelview matrixes in the ShaderProgram.
        // Because we want to render at point-blank range in this stage, we set
        // these to identity matrices.
        postprocessShader.setUniformMatrix("MVMatrix", new MatF4());
        postprocessShader.setUniformMatrix("PMatrix", new MatF4());

        // Stage the width and height.
        postprocessShader.setUniform("scrWidth", width);
        postprocessShader.setUniform("scrHeight", height);

        try {
            // Load all staged variables into the GPU, check for errors and
            // omissions.
            postprocessShader.use(gl);

            // Call the model's draw method, this links the model's VertexBuffer
            // to
            // the ShaderProgram and then calls the OpenGL draw method.
            FSQ_postprocess.draw(gl, postprocessShader);
        } catch (final UninitializedException e) {
            e.printStackTrace();
        }
    }

    // The reshape method is automatically called by the openGL animator if the
    // window holding the OpenGL 'canvas' is resized.
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        // Get the Opengl context from the drawable, and make it current, so
        // we can see it and draw on it. I've never seen this fail, but there is
        // error checking anyway.
        try {
            final int status = drawable.getContext().makeCurrent();
            if ((status != GLContext.CONTEXT_CURRENT)
                    && (status != GLContext.CONTEXT_CURRENT_NEW)) {
                System.err.println("Error swapping context to onscreen.");
            }
        } catch (final GLException e) {
            System.err.println("Exception while swapping context to onscreen.");
            e.printStackTrace();
        }

        // Once we have the context current, we can extract the OpenGL instance
        // from it. We have defined a OpenGL 3.0 instance in the
        // ESightNewtWindow by adding the line
        // glp = GLProfile.get(GLProfile.GL3);
        // Therefore, we extract a GL3 instance, so we cannot make any
        // unfortunate mistakes (calls to methods that are undefined for this
        // version).
        final GL3 gl = GLContext.getCurrentGL().getGL3();

        // set the new canvas size and aspect ratio in the global variables.
        canvasWidth = GLContext.getCurrent().getGLDrawable().getWidth();
        canvasHeight = GLContext.getCurrent().getGLDrawable().getHeight();
        aspect = (float) canvasWidth / (float) canvasHeight;

        // Resize the FrameBuffer Objects that we use for intermediate stages.
        axesFBO.delete(gl);
        axesFBO = new FBO(w, h, GL.GL_TEXTURE0);
        axesFBO.init(gl);

        // Resize the PixelBuffer Object that can be used for screenshots.
        finalPBO.delete(gl);
        finalPBO = new IntPBO(w, h);
        finalPBO.init(gl);
    }

    // This dispose method is called when the OpenGL 'canvas' is destroyed. It
    // is used for cleanup.
    @Override
    public void dispose(GLAutoDrawable drawable) {
        // Get the Opengl context from the drawable, and make it current, so
        // we can see it and draw on it. I've never seen this fail, but there is
        // error checking anyway.
        try {
            final int status = drawable.getContext().makeCurrent();
            if ((status != GLContext.CONTEXT_CURRENT)
                    && (status != GLContext.CONTEXT_CURRENT_NEW)) {
                System.err.println("Error swapping context to onscreen.");
            }
        } catch (final GLException e) {
            System.err.println("Exception while swapping context to onscreen.");
            e.printStackTrace();
        }

        // Once we have the context current, we can extract the OpenGL instance
        // from it. We have defined a OpenGL 3.0 instance in the
        // ESightNewtWindow by adding the line
        // glp = GLProfile.get(GLProfile.GL3);
        // Therefore, we extract a GL3 instance, so we cannot make any
        // unfortunate mistakes (calls to methods that are undefined for this
        // version).
        final GL3 gl = GLContext.getCurrentGL().getGL3();

        // Delete the FramBuffer Objects.
        axesFBO.delete(gl);
        finalPBO.delete(gl);

        // Let the ShaderProgramLoader clean up. This deletes all of the
        // ShaderProgram instances as well.
        loader.cleanup(gl);
    }

    public InputHandler getInputHandler() {
        return inputHandler;
    }
}
