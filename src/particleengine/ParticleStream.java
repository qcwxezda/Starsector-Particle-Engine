package particleengine;

class ParticleStream<T extends IEmitter> {
    T emitter;
    final int particlesPerBurst;
    final float particlesPerSecond;
    int currentIndex = 0;
    float currentCount = 0;
    boolean finished = false;
    final Float deathTime;
    final Particles.StreamAction<T> doBeforeGenerating;
    final Particles.StreamAction<T> doWhenFinished;

    ParticleStream(
            T emitter,
            int particlesPerBurst,
            float particlesPerSecond,
            Float deathTime,
            Particles.StreamAction<T> doBeforeGenerating,
            Particles.StreamAction<T> doWhenFinished) {
        this.emitter = emitter;
        this.particlesPerBurst = particlesPerBurst;
        this.particlesPerSecond = particlesPerSecond;
        this.deathTime = deathTime;
        this.doBeforeGenerating = doBeforeGenerating;
        this.doWhenFinished = doWhenFinished;
    }

    void advance(float amount) {
        currentCount += amount * particlesPerSecond;
        if (currentCount >= particlesPerBurst && (doBeforeGenerating == null || (finished = doBeforeGenerating.apply(emitter)))) {
            Particles.burst(emitter, particlesPerBurst, currentIndex);
            currentIndex += particlesPerBurst;
            currentCount -= particlesPerBurst;
        }
    }

    void finish() {
        doWhenFinished.apply(emitter);
    }
}
