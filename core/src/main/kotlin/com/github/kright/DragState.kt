package com.github.kright

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

/**
 * Class that encapsulates all state and functionality related to dragging objects.
 */
class DragState(
    private val camera: Camera, 
    private val modelSelection: ModelsSelection,
    val draggedModel: ModelInstance,
    private val draggedAxes: MovementAxes,
    private val initialMousePos: Vector2,
) {
    // Vector objects for calculations
    private val tempVector = Vector3()
    private val dragVector = Vector3()

    /**
     * Updates the dragging operation with the current mouse position.
     * Applies movement to all selected models.
     */
    fun updateDragging(currentX: Int, currentY: Int) {
        // Calculate mouse movement
        val deltaX = currentX - initialMousePos.x
        val deltaY = initialMousePos.y - currentY  // Invert Y since screen coordinates are Y-down
        
        // Calculate drag scale based on camera distance
        val dragScale = calculateDragScale(draggedModel)
        
        // Set movement vector based on selected axes
        dragVector.set(0f, 0f, 0f)
        if (draggedAxes.x) dragVector.x = deltaX * dragScale
        if (draggedAxes.y) dragVector.y = deltaY * dragScale
        if (draggedAxes.z) dragVector.z = -deltaX * dragScale

        
        // Apply movement to all selected models
        for (selectedModel in modelSelection.selected) {
            selectedModel.transform.getTranslation(tempVector)
            tempVector.add(dragVector)
            selectedModel.transform.setTranslation(tempVector)
        }
        
        // Update initial position for next frame
        initialMousePos.set(currentX.toFloat(), currentY.toFloat())
    }
    
    /**
     * Calculates the appropriate scale factor for dragging based on the model's distance from the camera.
     */
    private fun calculateDragScale(model: ModelInstance): Float {
        // Get the model's position in world space
        val worldPos = model.transform.getTranslation(tempVector)
        
        // Project the world position to screen space
        val screenPos = camera.project(Vector3(worldPos))
        
        // Calculate the distance from camera to model
        val distance = camera.position.dst(worldPos)
        
        // Scale factor based on distance (farther objects need larger movement)
        // This is a simple approximation, can be adjusted for better feel
        return distance * 0.003f
    }
}