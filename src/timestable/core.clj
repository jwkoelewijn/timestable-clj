(ns timestable.core
  (:import java.lang.Math)
  (:gen-class))


;; A point is a map with an x and an y
(defn new-point [x y]
  {:x x :y y})

;; A line is a vector of two points
(defn new-line [from to]
  [from to])

(defn new-circle [origin radius]
  {:origin origin :radius radius})

;; returns the angle between two points on the circle
(defn- inner-angle [n]
  (/ (* 2 Math/PI) ^float n))

;; Distribute n points evenly on the given circle, returns a collection of n points
(defn distribute-points [n {:keys [radius] :as circle}]
  (let [angle (inner-angle n)]
    (loop [nth 0
           points []]
      (let [point (new-point (* radius (Math/sin (* angle nth))) (* radius (Math/cos (* angle nth))))]
        (if (< nth (dec n))
          (recur (inc nth) (conj points point))
          (conj points point))))))

(defn multiplication-lines [factor points]
  (loop [n 0
         lines []]
    (let [line (new-line (get points n) (get points (int (Math/floor (mod (* factor n) (count points))))))]
      (if (< n (dec (count points)))
        (recur (inc n) (conj lines line))
        (conj lines line)))))

;;; UI stuff
(import
  '(java.awt Color Graphics2D Graphics Dimension RenderingHints)
  '(java.awt.event KeyListener)
  '(java.awt.image BufferedImage)
  '(javax.swing JPanel JFrame))

(def scale 6)
(def dim 120)
(def animation-sleep-ms 45)

(defn scale-point [point n]
  {:x (* n (:x point)) :y (* n (:y point))})

(defn scale-line [line n]
  [(scale-point (first line) n) (scale-point (last line) n)])

(defn move-point [point amount]
  {:x (+ amount (:x point)) :y (+ amount (:y point))})

(defn move-line [line amount]
  [(move-point (first line) amount) (move-point (last line) amount)])

(defn factor->color [factor]
  (let [r (int (+ (* (Math/sin factor) 127) 128))
        g (int (+ (* (Math/sin (+ factor 2)) 127) 128))
        b (int (+ (* (Math/sin (+ factor 3)) 127) 128))]
    (new Color r g b)))

(defn set-color [g factor]
  (. g (setColor (factor->color factor))))

(defn render-line [#^Graphics g line factor]
  (set-color g factor)
  (let [scaled-line (scale-line line (/ scale 2))
        moved-line (move-line scaled-line (/ (* dim scale) 2))]
    (. g (drawLine (:x (first moved-line)) (:y (first moved-line)) (:x (last moved-line)) (:y (last moved-line))))))

(defn render-circle [g circle factor]
  (set-color g factor)
  (let [radius (:radius circle)
        origin (:origin circle)
        translated-origin (move-point origin (- (/ dim 2) 1))
        scaled-radius (+ (* scale radius) 1)]
    (. g (drawOval (:x translated-origin) (:y translated-origin) scaled-radius scaled-radius))))

(defn generate-lines [factor numpoints circle]
  (multiplication-lines factor (distribute-points numpoints circle)))

(def factor (atom 1.0))
(def increment (atom 0.05))
(def running (atom false))

(defn renderFactor [#^Graphics g factor]
  (let [title (format "Factor: %.4f" factor)
        metrics (. g (getFontMetrics))
        bounds (. metrics (getStringBounds title g))
        graphicsCenter (/ (* scale dim) 2)]
    (doto g
      (set-color factor)
      (.drawString title (int (- graphicsCenter (/ (. bounds (getWidth)) 2))) (int (- (* dim scale) (/ dim 4)))))))

(defn render [#^Graphics2D g circle]
  (let [img (new BufferedImage (* scale dim) (* scale dim) (. BufferedImage TYPE_INT_ARGB))
        bg (. img (getGraphics))
        fact @factor]
    (doto bg
      (.setRenderingHint RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
      (.setRenderingHint RenderingHints/KEY_RENDERING RenderingHints/VALUE_RENDER_QUALITY)
      (.setRenderingHint RenderingHints/KEY_TEXT_ANTIALIASING RenderingHints/VALUE_TEXT_ANTIALIAS_OFF)
      (.setColor (. Color black))
      (.fillRect 0 0 (. img (getWidth)) (. img (getHeight))))
    (dorun
      (for [line (generate-lines fact 600 circle)]
        (render-line bg line fact)))
    (renderFactor bg fact)
    (render-circle bg circle fact)
    (. g (drawImage img 0 0 nil))
    (. bg (dispose))))

(declare animator)
(declare animation)
(declare set-factor!)

(defn handle-key [keychar]
  (condp = keychar
    \q (System/exit 0)
    \+ (swap! increment #(* % 1.1))
    \- (swap! increment #(* % 0.9))
    \= (swap! increment (fn [_] 0.05))
    \space (do
            (swap! running #(not %))
            (send-off animator animation))
    \1 (set-factor! 1.0)
    \2 (set-factor! 2.0)
    \3 (set-factor! 3.0)
    \4 (set-factor! 4.0)
    \5 (set-factor! 5.0)
    \6 (set-factor! 6.0)
    \7 (set-factor! 7.0)
    \8 (set-factor! 8.0)
    \9 (set-factor! 9.0)
    \0 (set-factor! 10.0)
    nil))

(def panel
  (let [circle (new-circle (new-point 0 0) (- dim 20))]
    (doto (proxy [JPanel KeyListener] []
                  (paint [g] (render g circle))
                  (keyPressed [e]
                    (handle-key (.getKeyChar e)))
                  (keyReleased [e])
                  (keyTyped [e]))
          (.setPreferredSize (new Dimension
                                   (* scale dim)
                                   (* scale dim))))))

(def frame (doto (new JFrame) (.addKeyListener panel) (.add panel) .pack .show))

(defn set-factor! [#^double newfactor]
  (swap! factor (fn [_] newfactor))
  (. panel (repaint)))

(def animator (agent nil))

(defn animation [x]
  (when @running
    (send-off *agent* #'animation))
  (set-factor! (+ @factor @increment))
  (. Thread (sleep animation-sleep-ms))
  nil)



(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (send-off animator animation))
