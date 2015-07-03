/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.almasb.fxgl.physics;

import java.util.Optional;

import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.callbacks.RayCastCallback;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;

import com.almasb.fxgl.GameApplication;
import com.almasb.fxgl.entity.Entity;

import javafx.geometry.Point2D;

/**
 * Manages physics entities and performs the physics tick
 *
 * Contains several static and instance methods
 * to convert pixels coordinates to meters and vice versa
 *
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 * @version 1.0
 *
 */
public final class PhysicsManager {

    private static final float TIME_STEP = 1 / 60.0f;

    private GameApplication app;

    private World world = new World(new Vec2(0, -10));

    public PhysicsManager(GameApplication app) {
        this.app = app;
        world.setContactListener(new ContactListener() {

            @Override
            public void beginContact(Contact contact) {
                PhysicsEntity e1 = (PhysicsEntity) contact.getFixtureA().getBody().getUserData();
                PhysicsEntity e2 = (PhysicsEntity) contact.getFixtureB().getBody().getUserData();

                app.triggerCollision(e1, e2);
            }

            @Override
            public void endContact(Contact contact) {

            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {}

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }

    /**
     * Set gravity for the physics world
     *
     * @param x
     * @param y
     */
    public void setGravity(double x, double y) {
        world.setGravity(new Vec2().addLocal((float)x,-(float)y));
    }

    /**
     * Do NOT call manually. This is the physics update tick
     *
     * @param now
     */
    public void onUpdate(long now) {
        world.step(TIME_STEP, 8, 3);

        for (Body body = world.getBodyList(); body != null; body = body.getNext()) {
            Entity e = (Entity) body.getUserData();
            e.setTranslateX(
                    Math.round(toPixels(
                            body.getPosition().x
                                    - toMeters(e.getLayoutBounds().getWidth() / 2))));
            e.setTranslateY(
                    Math.round(toPixels(
                            toMeters(app.getHeight()) - body.getPosition().y
                                    - toMeters(e.getLayoutBounds().getHeight() / 2))));
            e.setRotate(-Math.toDegrees(body.getAngle()));
        }
    }

    /**
     * Do NOT call manually. This is called by FXGL Application
     * to create a physics body in physics space (world)
     *
     * @param e
     */
    public void createBody(PhysicsEntity e) {
        double x = e.getTranslateX(),
                y = e.getTranslateY(),
                w = e.getLayoutBounds().getWidth(),
                h = e.getLayoutBounds().getHeight();

        if (e.fixtureDef.shape == null) {
            PolygonShape rectShape = new PolygonShape();
            rectShape.setAsBox(toMeters(w / 2), toMeters(h / 2));
            e.fixtureDef.shape = rectShape;
        }

        e.bodyDef.position.set(toMeters(x + w / 2), toMeters(app.getHeight() - (y + h / 2)));
        e.body = world.createBody(e.bodyDef);
        e.fixture = e.body.createFixture(e.fixtureDef);
        e.body.setUserData(e);
    }

    /**
     * Do NOT call manually. This is called by FXGL Application
     * to destroy a physics body in physics space (world)
     *
     * @param e
     */
    public void destroyBody(PhysicsEntity e) {
        world.destroyBody(e.body);
    }

    private EdgeCallback raycastCallback = new EdgeCallback();

    /**
     * Performs raycast from start to end
     *
     *
     * @param start world point in pixels
     * @param end world point in pixels
     * @return result of raycast
     */
    public RaycastResult raycast(Point2D start, Point2D end) {
        raycastCallback.reset();
        world.raycast(raycastCallback, toPoint(start), toPoint(end));

        PhysicsEntity entity = null;
        Point2D point = null;

        if (raycastCallback.fixture != null)
            entity = (PhysicsEntity) raycastCallback.fixture.getBody().getUserData();

        if (raycastCallback.point != null)
            point = toPoint(raycastCallback.point);

        return new RaycastResult(Optional.ofNullable(entity), Optional.ofNullable(point));
    }

    public static float toMeters(double pixels) {
        return (float)pixels * 0.05f;
    }

    public static float toPixels(double meters) {
        return (float)meters * 20f;
    }

    public static Vec2 toVector(Point2D v) {
        return new Vec2(toMeters(v.getX()), toMeters(-v.getY()));
    }

    public static Point2D toVector(Vec2 v) {
        return new Point2D(toPixels(v.x), toPixels(-v.y));
    }

    public Vec2 toPoint(Point2D p) {
        return new Vec2(toMeters(p.getX()), toMeters(app.getHeight() - p.getY()));
    }

    public Point2D toPoint(Vec2 p) {
        return new Point2D(toPixels(p.x), toPixels(toMeters(app.getHeight()) - p.y));
    }

    private static class EdgeCallback implements RayCastCallback {
        Fixture fixture;
        Vec2 point;
        //Vec2 normal;
        float bestFraction = 1.0f;

        @Override
        public float reportFixture(Fixture fixture, Vec2 point, Vec2 normal, float fraction) {
            PhysicsEntity e = (PhysicsEntity) fixture.getBody().getUserData();
            if (e.isRaycastIgnored())
                return 1;

            if (fraction < bestFraction) {
                this.fixture = fixture;
                this.point = point.clone();
                //this.normal = normal.clone();
                bestFraction = fraction;
            }

            return bestFraction;
        }

        void reset() {
            fixture = null;
            point = null;
            bestFraction = 1.0f;
        }
    }
}
