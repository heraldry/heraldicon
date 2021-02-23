(ns heraldry.coat-of-arms.line
  (:require ["svgpath" :as svgpath]
            [clojure.string :as s]
            [clojure.walk :as walk]
            [heraldry.coat-of-arms.catmullrom :as catmullrom]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.random :as random]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.tincture :as tincture]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(declare options)

(defn line-with-offset [length offset pattern-width pattern {:keys [reversed?]}]
  (let [offset-length (* offset pattern-width)
        repetitions (-> length
                        (- offset-length)
                        (/ pattern-width)
                        Math/ceil
                        int
                        inc)
        actual-length (-> (* repetitions pattern-width)
                          (cond->
                           (pos? offset-length) (+ offset-length)))
        start-offset (v/v (min 0 offset-length) 0)
        end-offset (-> (v/v actual-length 0)
                       (cond->
                        (neg? offset-length) (v/+ (v/v offset-length 0)))
                       (v/- (v/v length 0)))]
    {:line (-> []
               (cond->
                (and (not reversed?)
                     (pos? offset-length)) (into [["l" offset-length 0]]))
               (into (repeat repetitions pattern))
               (cond->
                (and reversed?
                     (pos? offset-length)) (into [["l" offset-length 0]]))
               (->> (apply merge))
               vec)
     :offset (min 0 offset-length)
     :length (-> (* repetitions pattern-width)
                 (+ (* offset-length 2)))
     :start-offset (if reversed?
                     (v/- (v/v 0 0)
                          end-offset)
                     start-offset)
     :end-offset (if reversed?
                   (v/- (v/v 0 0)
                        start-offset)
                   end-offset)}))

(defn straight
  {:display-name "Straight"}
  [line length _]
  (let [{fimbriation-mode :mode
         fimbriation-alignment :alignment
         fimbriation-thickness-1 :thickness-1
         fimbriation-thickness-2 :thickness-2} (:fimbriation line)
        base-line (cond
                    (and (not= fimbriation-mode :none)
                         (= fimbriation-alignment :even)) (-> fimbriation-thickness-1
                                                              (cond->
                                                               (#{:double} fimbriation-mode) (+ fimbriation-thickness-2))
                                                              (/ 2))
                    (and (= fimbriation-mode :single)
                         (= fimbriation-alignment :inside)) fimbriation-thickness-1
                    (and (= fimbriation-mode :double)
                         (= fimbriation-alignment :inside)) (+ fimbriation-thickness-1
                                                               fimbriation-thickness-2)
                    :else 0)]
    {:line ["h" length]
     :start-offset (v/v 0 base-line)
     :end-offset (v/v length base-line)
     :fimbriation-1 (when (#{:single :double} fimbriation-mode)
                      ["h" length])
     :fimbriation-1-offset (when (#{:single :double} fimbriation-mode)
                             (v/v 0 (- base-line
                                       fimbriation-thickness-1)))
     :fimbriation-2 (when (#{:double} fimbriation-mode)
                      ["h" length])
     :fimbriation-2-offset (when (#{:double} fimbriation-mode)
                             (v/v 0 (- base-line
                                       fimbriation-thickness-1
                                       fimbriation-thickness-2)))}))

(defn invected
  {:display-name "Invected"}
  [values length line-options]
  (let [{:keys [eccentricity
                height
                offset
                width]} (options/sanitize values (options values))
        radius-x (-> width
                     (/ 2)
                     (* (-> eccentricity
                            (min 1)
                            (* -0.5)
                            (+ 1.5))))
        radius-y (* radius-x height)]
    (line-with-offset
     length offset width
     ["a" radius-x radius-y 0 0 1 [width 0]]
     line-options)))

(defn engrailed
  {:display-name "Engrailed"}
  [values length line-options]
  (let [{:keys [eccentricity
                height
                offset
                width]} (options/sanitize values (options values))
        radius-x (-> width
                     (/ 2)
                     (* (-> eccentricity
                            (min 1)
                            (* -0.5)
                            (+ 1.5))))
        radius-y (* radius-x height)
        tx (-> width
               (/ 2))
        ty (-> (- 1 (/ (* tx tx)
                       (* radius-x radius-x)))
               Math/sqrt
               (* radius-y)
               (->> (- radius-y)))]
    (line-with-offset
     length offset width
     ["a" radius-x radius-y 0 0 0 [tx (- ty)]
      "a" radius-x radius-y 0 0 0 [tx ty]]
     line-options)))

(defn embattled
  {:display-name "Embattled"}
  [values length line-options]
  (let [{:keys [height
                offset
                width]} (options/sanitize values (options values))
        half-width (/ width 2)
        quarter-width (/ width 4)
        height (* half-width height)]
    (line-with-offset
     length offset width
     ["l"
      [quarter-width 0]
      [0 (- height)]
      [half-width 0]
      [0 height]
      [quarter-width 0]]
     line-options)))

(defn indented
  {:display-name "Indented"}
  [values length line-options]
  (let [{:keys [height
                offset
                width]} (options/sanitize values (options values))
        half-width (/ width 2)
        height (* half-width height)]
    (line-with-offset
     length offset width
     ["l"
      [half-width (- height)]
      [half-width height]]
     line-options)))

(defn dancetty
  {:display-name "Dancetty"}
  [values length {:keys [reversed?] :as line-options}]
  (let [{:keys [height
                offset
                width]} (options/sanitize values (options values))
        half-width (/ width 2)
        quarter-width (/ width 4)
        half-height (* quarter-width height)
        height (* half-height 2)]
    (line-with-offset
     length offset width
     (if reversed?
       ["l"
        [quarter-width half-height]
        [half-width (- height)]
        [quarter-width half-height]]
       ["l"
        [quarter-width (- half-height)]
        [half-width height]
        [quarter-width (- half-height)]])
     line-options)))

(defn wavy
  {:display-name "Wavy / undy"}
  [values length {:keys [reversed?] :as line-options}]
  (let [{:keys [eccentricity
                height
                offset
                width]} (options/sanitize values (options values))
        radius-x (-> width
                     (/ 4)
                     (* (-> eccentricity
                            (min 1)
                            (* -0.5)
                            (+ 1.5))))
        radius-y (* radius-x height)
        tx (-> width
               (/ 2))]
    (line-with-offset
     length offset width
     ["a" radius-x radius-y 0 0 (if reversed? 0 1) [tx 0]
      "a" radius-x radius-y 0 0 (if reversed? 1 0) [tx 0]]
     line-options)))

(defn dovetailed
  {:display-name "Dovetailed"}
  [values length line-options]
  (let [{:keys [eccentricity
                height
                offset
                width]} (options/sanitize values (options values))
        half-width (/ width 2)
        quarter-width (/ width 4)
        height (* half-width height)
        dx (-> width
               (/ 4)
               (* (-> eccentricity
                      (* 0.5)
                      (+ 0.2))))]
    (line-with-offset
     length offset width
     ["l"
      [(+ quarter-width
          dx) 0]
      [(* dx -2) (- height)]
      [(+ half-width
          dx
          dx) 0]
      [(* dx -2) height]
      [(+ quarter-width
          dx) 0]]
     line-options)))

(defn raguly
  {:display-name "Raguly"}
  [values length {:keys [reversed?] :as line-options}]
  (let [{:keys [eccentricity
                height
                offset
                width]} (options/sanitize values (options values))
        half-width (/ width 2)
        quarter-width (/ width 4)
        height (* half-width height)
        dx (-> width
               (/ 2)
               (* (-> eccentricity
                      (* 0.7)
                      (+ 0.3))))]
    (line-with-offset
     length offset width
     (if reversed?
       ["l"
        [quarter-width 0]
        [quarter-width (- height)]
        [half-width 0]
        [(- quarter-width) height]
        [quarter-width 0]]
       ["l"
        [quarter-width 0]
        [(- dx) (- height)]
        [half-width 0]
        [dx height]
        [quarter-width 0]])
     line-options)))

(defn urdy
  {:display-name "Urdy"}
  [values length {:keys [reversed?] :as line-options}]
  (let [{:keys [eccentricity
                height
                offset
                width]} (options/sanitize values (options values))
        quarter-width (/ width 4)
        pointy-height (* quarter-width
                         (* 2)
                         (* (-> eccentricity
                                (* 0.6)
                                (+ 0.2)))
                         (* height))
        middle-height (* quarter-width height)
        half-height (/ middle-height 2)]
    (line-with-offset
     length offset width
     (if reversed?
       ["l"
        [0 half-height]
        [quarter-width pointy-height]
        [quarter-width (- pointy-height)]
        [0 (- middle-height)]
        [quarter-width (- pointy-height)]
        [quarter-width pointy-height]
        [0 half-height]]
       ["l"
        [0 (- half-height)]
        [quarter-width (- pointy-height)]
        [quarter-width pointy-height]
        [0 middle-height]
        [quarter-width pointy-height]
        [quarter-width (- pointy-height)]
        [0 (- half-height)]])
     line-options)))

(def lines
  [#'straight
   #'invected
   #'engrailed
   #'embattled
   #'indented
   #'dancetty
   #'wavy
   #'dovetailed
   #'raguly
   #'urdy])

(def kinds-function-map
  (->> lines
       (map (fn [function]
              [(-> function meta :name keyword) function]))
       (into {})))

(def choices
  (->> lines
       (map (fn [function]
              [(-> function meta :display-name) (-> function meta :name keyword)]))))

(def line-map
  (util/choices->map choices))

(def fimbriation-choices
  [["None" :none]
   ["Single" :single]
   ["Double" :double]])

(def fimbriation-map
  (util/choices->map fimbriation-choices))

(def fimbriation-alignment-choices
  [["Even" :even]
   ["Outside" :outside]
   ["Inside" :inside]])

(def fimbriation-alignment-map
  (util/choices->map fimbriation-alignment-choices))

(def default-options
  {:type {:type :choice
          :choices choices
          :default :straight}
   :eccentricity {:type :range
                  :min 0
                  :max 1
                  :default 0.5}
   :height {:type :range
            :min 0.2
            :max 3
            :default 1}
   :width {:type :range
           :min 2
           :max 100
           :default 10}
   :offset {:type :range
            :min -1
            :max 3
            :default 0}
   :flipped? {:type :boolean
              :default false}
   :fimbriation {:mode {:type :choice
                        :choices fimbriation-choices
                        :default :none}
                 :alignment {:type :choice
                             :choices fimbriation-alignment-choices
                             :default :even}
                 :outline? {:type :boolean
                            :default false}
                 :thickness-1 {:type :range
                               :min 1
                               :max 10
                               :default 6}
                 :tincture-1 {:type :choice
                              :choices (-> [["None" :none]]
                                           (into tincture/choices))
                              :default :none}
                 :thickness-2 {:type :range
                               :min 1
                               :max 10
                               :default 3}
                 :tincture-2 {:type :choice
                              :choices (-> [["None" :none]]
                                           (into tincture/choices))
                              :default :none}}})

(defn options [line]
  (options/merge
   default-options
   (get {:straight {:eccentricity nil
                    :offset nil
                    :height nil
                    :width nil
                    :flipped? nil}
         :invected {:eccentricity {:default 1}}
         :engrailed {:eccentricity {:default 1}}
         :indented {:eccentricity nil}
         :embattled {:eccentricity nil}
         :dancetty {:width {:default 20}
                    :eccentricity nil}
         :wavy {:width {:default 20}}}
        (:type line))))

(defn jiggle [[previous
               {:keys [x y] :as current}
               _]]
  (let [dist (-> current
                 (v/- previous)
                 (v/abs))
        jiggle-radius (/ dist 4)
        dx (- (* (random/float) jiggle-radius)
              jiggle-radius)
        dy (- (* (random/float) jiggle-radius)
              jiggle-radius)]
    {:x (+ x dx)
     :y (+ y dy)}))

(defn squiggly-path [path & {:keys [seed]}]
  (random/seed (if seed
                 [seed path]
                 path))
  (let [points (-> path
                   svg/new-path
                   (svg/points :length))
        points (vec (concat [(first points)]
                            (map jiggle (partition 3 1 points))
                            [(last points)]))
        curve (catmullrom/catmullrom points)
        new-path (catmullrom/curve->svg-path-relative curve)]
    new-path))

(defn squiggly-paths [data]
  (walk/postwalk #(cond-> %
                    (vector? %) ((fn [v]
                                   (if (= (first v) :d)
                                     [:d (squiggly-path (second v))]
                                     v))))
                 data))

(defn create [{:keys [type] :or {type :straight} :as line} length & {:keys [angle flipped? extra render-options seed] :or {extra 50} :as line-options}]
  (let [line-function (get kinds-function-map type)
        line-data (line-function
                   line
                   length line-options)
        line-options-values (options/sanitize line (options line))
        line-flipped? (:flipped? line-options-values)
        adjusted-path (-> line-data
                          :line
                          svg/make-path
                          (->>
                           (str "M 0,0 "))
                          (cond->
                           (:squiggly? render-options) (squiggly-path :seed seed)))
        adjusted-fimbriation-1 (some-> line-data
                                       :fimbriation-1
                                       svg/make-path
                                       (->>
                                        (str "M 0,0 "))
                                       (cond->
                                        (:squiggly? render-options) (squiggly-path :seed [seed :fimbriation-1])))
        adjusted-fimbriation-2 (some-> line-data
                                       :fimbriation-2
                                       svg/make-path
                                       (->>
                                        (str "M 0,0 "))
                                       (cond->
                                        (:squiggly? render-options) (squiggly-path :seed [seed :fimbriation-2])))]
    (-> line-data
        (assoc :line
               (-> adjusted-path
                   svgpath
                   (cond->
                    (or (and flipped? (not line-flipped?))
                        (and (not flipped?) line-flipped?)) (.scale 1 -1))
                   (.rotate angle)
                   .toString))
        (assoc :fimbriation-1
               (some-> adjusted-fimbriation-1
                       svgpath
                       (cond->
                        (or (and flipped? (not line-flipped?))
                            (and (not flipped?) line-flipped?)) (.scale 1 -1))
                       (.rotate angle)
                       .toString))
        (assoc :fimbriation-2
               (some-> adjusted-fimbriation-2
                       svgpath
                       (cond->
                        (or (and flipped? (not line-flipped?))
                            (and (not flipped?) line-flipped?)) (.scale 1 -1))
                       (.rotate angle)
                       .toString))
        (update :start-offset (fn [p] (when p (v/rotate p angle))))
        (update :end-offset (fn [p] (when p (v/rotate p angle))))
        (update :line-offset (fn [p] (when p (v/rotate p angle))))
        (update :fimbriation-1-offset (fn [p] (when p (v/rotate p angle))))
        (update :fimbriation-2-offset (fn [p] (when p (v/rotate p angle)))))))

(defn translate [path dx dy]
  (-> path
      svgpath
      (.translate dx dy)
      .toString))

(defn stitch [path]
  ;; TODO: this can be improved, it already broke some things and caused unexpected behaviour,
  ;; because the 'e' was not part of the pattern
  (s/replace path #"^M[ ]*[0-9.e-]+[, -] *[0-9.e-]+" ""))
