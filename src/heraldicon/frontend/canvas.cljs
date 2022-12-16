(ns heraldicon.frontend.canvas
  (:require
   [cljs.core.async.interop :refer-macros [<p!]]
   [com.wsscode.async.async-cljs :refer [go-catch <?]]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
   [heraldicon.util.async :as util.async]
   [reagent.dom.server :as r-server]))

(defn draw-svg [svg-data target-width target-height]
  (go-catch
   (let [svg-data-str (r-server/render-to-static-markup
                       [:svg {:viewBox (str "0 0 " target-width " " target-height)
                              :version "1.1"
                              :xmlns "http://www.w3.org/2000/svg"
                              :width target-width
                              :height target-height}
                        svg-data])
         img (js/Image.)
         canvas (js/OffscreenCanvas. target-width target-height)
         blob (js/Blob. (clj->js [svg-data-str]) (clj->js {:type "image/svg+xml"}))
         url (js/window.URL.createObjectURL blob)]
     (<p! (util.async/promise-from-callback
           (fn [callback]
             (set! img.onload (fn []
                                (let [context (doto (.getContext canvas "2d")
                                                (.drawImage img 0 0))
                                      image-data (.getImageData context 0 0 target-width target-height)]
                                  (callback nil image-data))))
             (set! img.src url)))))))

(defn- get-painted-pixels [image]
  (let [pixels (.-data image)
        alphas (take-nth 4 (drop 3 pixels))]
    (mapv pos? alphas)))

(defn- painted-bounding-box [painted-pixels width]
  (let [painted-coords (keep-indexed (fn [index painted?]
                                       (when painted?
                                         (v/Vector. (mod index width)
                                                    (quot index width))))
                                     painted-pixels)]
    (-> (bb/from-points painted-coords)
        (update :max-x inc)
        (update :max-y inc))))

(defn- get-pixel [pixels width x y]
  (if (or (neg? x)
          (neg? y)
          (>= x width)
          (>= y (/ (count pixels) width)))
    0
    (get pixels (+ (* y width) x))))

(defn- trace-shape [pixels width {:keys [x y dir]
                                  :as edge} edges]
  #_(js/console.log :trace edge edges)
  (if (= (first edges) edge)
    edges
    (let [possible-edges (case dir
                           :right [{:x (inc x)
                                    :y (inc y)
                                    :dir :top}
                                   {:x x
                                    :y (inc y)
                                    :dir
                                    :right}
                                   {:x x
                                    :y y
                                    :dir :bottom}]
                           :left [{:x (dec x)
                                   :y (dec y)
                                   :dir :bottom}
                                  {:x x
                                   :y (dec y)
                                   :dir :left}
                                  {:x x
                                   :y y
                                   :dir :top}]
                           :top [{:x (inc x)
                                  :y (dec y)
                                  :dir :left}
                                 {:x (inc x)
                                  :y y
                                  :dir :top}
                                 {:x x
                                  :y y
                                  :dir :right}]
                           :bottom [{:x (dec x)
                                     :y (inc y)
                                     :dir :right}
                                    {:x (dec x)
                                     :y y
                                     :dir :bottom}
                                    {:x x
                                     :y y
                                     :dir :left}])
          new-edge (first (filter (fn [{:keys [x y]}]
                                    (= 1 (get-pixel pixels width x y)))
                                  possible-edges))]
      (recur pixels width new-edge (conj edges edge)))))

(defn- add-edges-for-lookup [shapes-data edges shape-index]
  (update shapes-data :edge-map
          into
          (map (fn [edge]
                 [edge shape-index]))
          edges))

(defn- enter-shape [{:keys [edge-map shapes]
                     :as shapes-data} pixels width x y]
  (let [edge {:x x
              :y y
              :dir :left}]
    (if-let [shape-index (get edge-map edge)]
      (assoc shapes-data :current shape-index)
      (let [new-shape (trace-shape pixels width edge [])
            shape-index (count shapes)]
        (-> shapes-data
            (assoc :current shape-index)
            (assoc-in [:shapes shape-index] [new-shape])
            (add-edges-for-lookup new-shape shape-index))))))

(defn- leave-shape [{:keys [current edge-map]
                     :as shapes-data} pixels width x y]
  (let [edge {:x (dec x)
              :y y
              :dir :right}]
    (if (get edge-map edge)
      (assoc shapes-data :current nil)
      (let [new-shape (trace-shape pixels width edge [])]
        (-> shapes-data
            (assoc :current nil)
            (update-in [:shapes current] conj new-shape)
            (add-edges-for-lookup new-shape current))))))

(defn- build-shapes [pixels width]
  (let [height (/ (count pixels) width)]
    (loop [x 0
           y 0
           {:keys [current]
            :as shapes-data} {:current nil
                              :shapes {}
                              :edge-map {}}]
      (if (>= y height)
        (:shapes shapes-data)
        (let [[nx ny] (if (>= x width)
                        [0 (inc y)]
                        [(inc x) y])
              new-shapes-data (cond
                                (and (not current)
                                     (= 1 (get-pixel pixels width x y))) (enter-shape shapes-data pixels width x y)
                                (and current
                                     (= 0 (get-pixel pixels width x y))) (leave-shape shapes-data pixels width x y)
                                :else shapes-data)]
          (recur nx ny new-shapes-data))))))

(defn- painted-shapes [painted-pixels width]
  (let [shapes (build-shapes painted-pixels width)]))

(def ^:private max-depth 3)

(defn svg-bounding-box [svg-data base-bounding-box & {:keys [depth]
                                                      :or {depth 0}}]
  (go-catch
   (let [target-width 256
         target-height 256
         width (bb/width base-bounding-box)
         height (bb/height base-bounding-box)
         top-left (bb/top-left base-bounding-box)
         scale (min (/ target-width width)
                    (/ target-height height))
         shifted-svg-data [:g {:transform (str "scale(" scale "," scale ")"
                                               "translate(" (- (:x top-left)) "," (- (:y top-left)) ")")}
                           svg-data]
         image (<? (draw-svg shifted-svg-data target-width target-height))
         width (.-width image)
         painted-pixels (get-painted-pixels image)
         bounding-box (painted-bounding-box painted-pixels width)
         empty-bounding-box? (or (zero? (bb/width bounding-box))
                                 (zero? (bb/height bounding-box)))]
     (if empty-bounding-box?
       base-bounding-box
       (let [bounding-box (if (>= depth max-depth)
                            bounding-box
                            (<? (svg-bounding-box shifted-svg-data bounding-box :depth (inc depth))))]
         (-> bounding-box
             (bb/scale (/ 1 scale))
             (bb/translate top-left)))))))

(comment

  (go-catch
   (js/console.log :test (<? (svg-bounding-box [:rect {:x 10.34
                                                       :y 10.3
                                                       :width 8.12
                                                       :height 6.12
                                                       :fill "red"}]
                                               (bb/from-vector-and-size v/zero 256 256)))))

  (do
    (def test-pixels
      [40
       [1 1 0 0 1 1 1 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
        1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
        1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0
        1 1 0 0 1 1 1 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0
        1 1 0 1 0 1 1 0 0 0 0 0 0 0 0 0 0 1 1 0 1 1 1 1 1 1 1 1 0 1 1 0 0 0 0 0 0 0 0 0
        1 0 0 1 1 1 0 0 0 0 0 0 0 0 0 0 0 1 1 0 1 0 0 0 1 1 1 1 0 1 1 0 0 0 0 0 0 0 0 0
        0 0 0 0 1 1 1 1 1 1 0 0 0 0 0 0 0 1 1 0 1 0 1 0 1 0 0 1 0 1 1 0 0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 1 0 1 0 1 0 1 0 0 1 0 1 1 0 0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 1 0 1 0 1 0 1 1 0 1 0 1 1 0 0 0 0 0 0 0 0 0
        0 0 0 0 0 0 1 1 1 0 0 0 0 0 0 0 0 1 1 0 1 0 0 0 1 1 1 1 0 1 1 0 0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 0 1 1 1 1 1 1 1 1 0 1 1 0 0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]])

    #_(try
        (let [[width pixels] test-pixels
              shape (trace-shape pixels width {:x 0
                                               :y 0
                                               :dir :top} [])
              #_#_shapes (build-shapes pixels width)]
          shape
        ;;
          )
        (catch :default e
          (js/console.log :error e)))

    (try
      (let [[width pixels] test-pixels
            shapes (build-shapes pixels width)]
        shapes
        ;;
        )
      (catch :default e
        (js/console.log :error e))))

;;
  )
