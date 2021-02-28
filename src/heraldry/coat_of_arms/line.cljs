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
        repetitions   (-> length
                          (- offset-length)
                          (/ pattern-width)
                          Math/ceil
                          int
                          inc)
        actual-length (-> (* repetitions pattern-width)
                          (cond->
                              (pos? offset-length) (+ offset-length)))
        line-start    (v/v (min 0 offset-length) 0)
        line-end      (-> (v/v actual-length 0)
                          (cond->
                              (neg? offset-length) (v/+ (v/v offset-length 0)))
                          (v/- (v/v length 0)))]
    {:line       (-> []
                     (cond->
                         (and (not reversed?)
                              (pos? offset-length)) (into [["l" offset-length 0]]))
                     (into (repeat repetitions pattern))
                     (cond->
                         (and reversed?
                              (pos? offset-length)) (into [["l" offset-length 0]]))
                     (->> (apply merge))
                     vec)
     :offset     (min 0 offset-length)
     :length     (-> (* repetitions pattern-width)
                     (+ (* offset-length 2)))
     :line-start (if reversed?
                   (v/- (v/v 0 0)
                        line-end)
                   line-start)
     :line-end   (if reversed?
                   (v/- (v/v 0 0)
                        line-start)
                   line-end)}))

(defn line-with-offset2 [{fimbriation   :fimbriation
                          pattern-width :width
                          line-offset   :offset
                          :as           line}
                         length line-function {:keys [reversed?
                                                      joint-angle]
                                               :as   line-options}]
  (let [{fimbriation-mode        :mode
         fimbriation-alignment   :alignment
         fimbriation-thickness-1 :thickness-1
         fimbriation-thickness-2 :thickness-2} fimbriation

        base-line              (cond
                                 (and (not= fimbriation-mode :none)
                                      (= fimbriation-alignment :even))   (-> fimbriation-thickness-1
                                                                             (cond->
                                                                                 (#{:double} fimbriation-mode) (+ fimbriation-thickness-2))
                                                                             (/ 2))
                                 (and (= fimbriation-mode :single)
                                      (= fimbriation-alignment :inside)) fimbriation-thickness-1
                                 (and (= fimbriation-mode :double)
                                      (= fimbriation-alignment :inside)) (+ fimbriation-thickness-1
                                                                            fimbriation-thickness-2)
                                 :else                                   0)
        fimbriation-1-line     (- base-line
                                  fimbriation-thickness-1)
        fimbriation-2-line     (- base-line
                                  fimbriation-thickness-1
                                  fimbriation-thickness-2)
        offset-x-factor        (if joint-angle
                                 (-> joint-angle
                                     (/ 2)
                                     (* Math/PI)
                                     (/ 180)
                                     Math/tan
                                     (->> (/ 1)))
                                 0)
        line-start-x           (* base-line
                                  offset-x-factor)
        fimbriation-1-offset-x (* fimbriation-1-line
                                  offset-x-factor)
        fimbriation-2-offset-x (* fimbriation-2-line
                                  offset-x-factor)

        {line-pattern        :pattern
         line-pattern-offset :offset}          (line-function line base-line line-options)
        {fimbriation-1-pattern        :pattern
         fimbriation-1-pattern-offset :offset} (line-function line fimbriation-1-line (-> line-options
                                                                                          (update :reversed? not)))
        {fimbriation-2-pattern        :pattern
         fimbriation-2-pattern-offset :offset} (line-function line fimbriation-2-line (-> line-options
                                                                                          (update :reversed? not)))
        offset-length                          (* line-offset pattern-width)
        repetitions                            (-> length
                                                   (- offset-length)
                                                   (/ pattern-width)
                                                   Math/ceil
                                                   int
                                                   inc)
        actual-length                          (-> (* repetitions pattern-width)
                                                   (cond->
                                                       (pos? offset-length) (+ offset-length)))
        line-start                             (v/v (min 0 offset-length) 0)
        line-end                               (-> (v/v actual-length 0)
                                                   (cond->
                                                       (neg? offset-length) (v/+ (v/v offset-length 0)))
                                                   (v/- (v/v length 0)))

        line-start         (if reversed?
                             (v/* line-start -1)
                             line-start)
        line-end           (if reversed?
                             (v/* line-end -1)
                             line-end)
        line-data          {:line       (-> []
                                            (cond->
                                                (not reversed?) (into [["h" (if (pos? offset-length)
                                                                              (+ line-start-x
                                                                                 offset-length)
                                                                              line-start-x)]]))
                                            (into (repeat repetitions line-pattern))
                                            (cond->
                                                reversed? (into [["h" (if (pos? offset-length)
                                                                        (+ line-start-x
                                                                           offset-length)
                                                                        line-start-x)]]))
                                            (->> (apply merge))
                                            vec)
                            :line-start (v/+ (v/v (- line-start-x)
                                                  base-line)
                                             line-start
                                             line-pattern-offset)
                            :line-end   (v/+ (v/v 0 base-line)
                                             line-end
                                             line-pattern-offset)}
        fimbriation-1-data (when (#{:single :double} fimbriation-mode)
                             {:fimbriation-1       (-> []
                                                       (cond->
                                                           reversed?  (into [["h" (-> fimbriation-1-offset-x
                                                                                      (cond-> (pos? offset-length) (+ offset-length)))]]))
                                                       (into (repeat repetitions fimbriation-1-pattern))
                                                       (cond->
                                                           (not reversed?) (into [["h" (-> fimbriation-1-offset-x
                                                                                           (cond-> (pos? offset-length) (+ offset-length)))]]))
                                                       (->> (apply merge))
                                                       vec)
                              :fimbriation-1-start (v/+ (v/v fimbriation-1-offset-x
                                                             fimbriation-1-line)
                                                        line-start
                                                        fimbriation-1-pattern-offset)
                              :fimbriation-1-end   (v/+ (v/v 0
                                                             fimbriation-1-line)
                                                        line-end
                                                        fimbriation-1-pattern-offset)})
        fimbriation-2-data (when (#{:double} fimbriation-mode)
                             {:fimbriation-2       (-> []
                                                       (cond->
                                                           reversed?  (into [["h" (-> fimbriation-2-offset-x
                                                                                      (cond-> (pos? offset-length) (+ offset-length)))]]))
                                                       (into (repeat repetitions fimbriation-2-pattern))
                                                       (cond->
                                                           (not reversed?)  (into [["h" (-> fimbriation-2-offset-x
                                                                                            (cond-> (pos? offset-length) (+ offset-length)))]]))
                                                       (->> (apply merge))
                                                       vec)
                              :fimbriation-2-start (v/+ (v/v fimbriation-2-offset-x
                                                             fimbriation-2-line)
                                                        line-start
                                                        fimbriation-2-pattern-offset)
                              :fimbriation-2-end   (v/+ (v/v 0
                                                             fimbriation-2-line)
                                                        line-end
                                                        fimbriation-2-pattern-offset)})
        line-data          (merge line-data fimbriation-1-data fimbriation-2-data)]
    (cond-> line-data
      reversed?                                   (->
                                                   (assoc :line-start (:line-end line-data))
                                                   (assoc :line-end (:line-start line-data)))
      (and reversed?
           (#{:single :double} fimbriation-mode)) (->
                                                   (assoc :fimbriation-1-start (:fimbriation-1-end line-data))
                                                   (assoc :fimbriation-1-end (:fimbriation-1-start line-data)))
      (and reversed?
           (#{:double} fimbriation-mode))         (->
                                                   (assoc :fimbriation-2-start (:fimbriation-2-end line-data))
                                                   (assoc :fimbriation-2-end (:fimbriation-2-start line-data))))))

(defn straight
  {:display-name "Straight"}
  [line length {:keys [joint-angle reversed?]}]
  (let [{fimbriation-mode        :mode
         fimbriation-alignment   :alignment
         fimbriation-thickness-1 :thickness-1
         fimbriation-thickness-2 :thickness-2} (:fimbriation line)

        base-line              (cond
                                 (and (not= fimbriation-mode :none)
                                      (= fimbriation-alignment :even))   (-> fimbriation-thickness-1
                                                                             (cond->
                                                                                 (#{:double} fimbriation-mode) (+ fimbriation-thickness-2))
                                                                             (/ 2))
                                 (and (= fimbriation-mode :single)
                                      (= fimbriation-alignment :inside)) fimbriation-thickness-1
                                 (and (= fimbriation-mode :double)
                                      (= fimbriation-alignment :inside)) (+ fimbriation-thickness-1
                                                                            fimbriation-thickness-2)
                                 :else                                   0)
        fimbriation-1-line     (- base-line
                                  fimbriation-thickness-1)
        fimbriation-2-line     (- base-line
                                  fimbriation-thickness-1
                                  fimbriation-thickness-2)
        offset-x-factor        (if joint-angle
                                 (-> joint-angle
                                     (/ 2)
                                     (* Math/PI)
                                     (/ 180)
                                     Math/tan
                                     (->> (/ 1)))
                                 0)
        line-start-x           (* base-line
                                  offset-x-factor)
        fimbriation-1-offset-x (* fimbriation-1-line
                                  offset-x-factor)
        fimbriation-2-offset-x (* fimbriation-2-line
                                  offset-x-factor)

        line-data {:line                ["h" (+ length
                                                line-start-x)]
                   :line-start          (v/v (- line-start-x)
                                             base-line)
                   :line-end            (v/v 0
                                             base-line)
                   :fimbriation-1       (when (#{:single :double} fimbriation-mode)
                                          ["h" (+ length
                                                  fimbriation-1-offset-x)])
                   :fimbriation-1-start (when (#{:single :double} fimbriation-mode)
                                          (v/v fimbriation-1-offset-x
                                               fimbriation-1-line))
                   :fimbriation-1-end   (when (#{:single :double} fimbriation-mode)
                                          (v/v 0
                                               fimbriation-1-line))
                   :fimbriation-2       (when (#{:double} fimbriation-mode)
                                          ["h" (+ length
                                                  fimbriation-2-offset-x)])
                   :fimbriation-2-start (when (#{:double} fimbriation-mode)
                                          (v/v fimbriation-2-offset-x
                                               fimbriation-2-line))
                   :fimbriation-2-end   (when (#{:double} fimbriation-mode)
                                          (v/v 0
                                               fimbriation-2-line))}]
    (cond-> line-data
      reversed?                                   (->
                                                   (assoc :line-start (:line-end line-data))
                                                   (assoc :line-end (:line-start line-data)))
      (and reversed?
           (#{:single :double} fimbriation-mode)) (->
                                                   (assoc :fimbriation-1-start (:fimbriation-1-end line-data))
                                                   (assoc :fimbriation-1-end (:fimbriation-1-start line-data)))
      (and reversed?
           (#{:double} fimbriation-mode))         (->
                                                   (assoc :fimbriation-2-start (:fimbriation-2-end line-data))
                                                   (assoc :fimbriation-2-end (:fimbriation-2-start line-data))))))

(defn invected
  {:display-name "Invected"}
  [{:keys [eccentricity
           height
           width]}
   _fimbriation-offset
   line-options]
  (let [radius-x (-> width
                     (/ 2)
                     (* (-> eccentricity
                            (min 1)
                            (* -0.5)
                            (+ 1.5))))
        radius-y (* radius-x height)]
    {:pattern ["a" radius-x radius-y 0 0 1 [width 0]]
     :offset  v/zero}))

(defn engrailed
  {:display-name "Engrailed"}
  [{:keys [eccentricity
           height
           width]}
   _fimbriation-offset
   line-options]
  (let [radius-x (-> width
                     (/ 2)
                     (* (-> eccentricity
                            (min 1)
                            (* -0.5)
                            (+ 1.5))))
        radius-y (* radius-x height)
        tx       (-> width
                     (/ 2))
        ty       (-> (- 1 (/ (* tx tx)
                             (* radius-x radius-x)))
                     Math/sqrt
                     (* radius-y)
                     (->> (- radius-y)))]
    {:pattern ["a" radius-x radius-y 0 0 0 [tx (- ty)]
               "a" radius-x radius-y 0 0 0 [tx ty]]
     :offset  v/zero}))

(defn embattled
  {:display-name "Embattled"}
  [{:keys [height
           width]}
   fimbriation-offset
   line-options]
  (let [half-width    (/ width 2)
        quarter-width (/ width 4)
        height        (* half-width height)
        filled?       (>= (Math/abs fimbriation-offset) quarter-width)]
    {:pattern (if filled?
                ["h" width]
                ["l"
                 [(+ quarter-width
                     fimbriation-offset) 0]
                 [0 (- height)]
                 [(- half-width
                     (* 2 fimbriation-offset)) 0]
                 [0 height]
                 [(+ quarter-width
                     fimbriation-offset) 0]])
     :offset  (if filled?
                (v/v 0 (- height))
                (v/v 0 0))}))

(defn indented
  {:display-name "Indented"}
  [{:keys [height
           width]}
   _fimbriation-offset
   line-options]
  (let [half-width (/ width 2)
        height     (* half-width height)]
    {:pattern ["l"
               [half-width (- height)]
               [half-width height]]
     :offset  v/zero}))

(defn dancetty
  {:display-name "Dancetty"}
  [{:keys [height
           width]}
   _fimbriation-offset
   {:keys [reversed?] :as line-options}]
  (let [half-width    (/ width 2)
        quarter-width (/ width 4)
        half-height   (* quarter-width height)
        height        (* half-height 2)]
    {:pattern (if reversed?
                ["l"
                 [quarter-width half-height]
                 [half-width (- height)]
                 [quarter-width half-height]]
                ["l"
                 [quarter-width (- half-height)]
                 [half-width height]
                 [quarter-width (- half-height)]])
     :offset  v/zero}))

(defn wavy
  {:display-name "Wavy / undy"}
  [{:keys [eccentricity
           height
           width]}
   _fimbriation-offset
   {:keys [reversed?] :as line-options}]
  (let [radius-x (-> width
                     (/ 4)
                     (* (-> eccentricity
                            (min 1)
                            (* -0.5)
                            (+ 1.5))))
        radius-y (* radius-x height)
        tx       (-> width
                     (/ 2))]
    {:pattern ["a" radius-x radius-y 0 0 (if reversed? 0 1) [tx 0]
               "a" radius-x radius-y 0 0 (if reversed? 1 0) [tx 0]]
     :offset  v/zero}))

(defn dovetailed
  {:display-name "Dovetailed"}
  [{:keys [eccentricity
           height
           width]}
   fimbriation-offset
   line-options]
  (let [half-width    (/ width 2)
        quarter-width (/ width 4)
        height        (* half-width height)
        dx            (-> width
                          (/ 4)
                          (* (-> eccentricity
                                 (* 0.5)
                                 (+ 0.2))))
        shift-x       (-> fimbriation-offset
                          (* dx 2)
                          (/ height))
        filled?       (>= (Math/abs fimbriation-offset) (- quarter-width dx (- shift-x)))]
    {:pattern (if filled?
                ["h" width]
                ["l"
                 [(+ quarter-width
                     dx
                     fimbriation-offset
                     shift-x) 0]
                 [(* dx -2) (- height)]
                 [(+ half-width
                     dx
                     dx
                     (* -2 fimbriation-offset)
                     (* -2 shift-x)) 0]
                 [(* dx -2) height]
                 [(+ quarter-width
                     dx
                     fimbriation-offset
                     shift-x) 0]])
     :offset  (if filled?
                (v/v 0 (- height))
                v/zero)}))

(defn raguly
  {:display-name "Raguly"}
  [{:keys [eccentricity
           height
           width]}
   fimbriation-offset
   {:keys [reversed?] :as line-options}]
  (let [half-width    (/ width 2)
        quarter-width (/ width 4)
        height        (* half-width height)
        dx            (-> width
                          (/ 2)
                          (* (-> eccentricity
                                 (* 0.7)
                                 (+ 0.3))))
        filled?       (>= (Math/abs fimbriation-offset) quarter-width)
        shift-x       (-> fimbriation-offset
                          (* dx)
                          (/ height))]
    {:pattern (if filled?
                ["h" width]
                (if reversed?
                  ["l"
                   [(+ quarter-width
                       fimbriation-offset
                       (- shift-x)) 0]
                   [dx (- height)]
                   [(+ half-width
                       (* -2 fimbriation-offset)) 0]
                   [(- dx) height]
                   [(+ quarter-width
                       fimbriation-offset
                       shift-x) 0]]
                  ["l"
                   [(+ quarter-width
                       fimbriation-offset
                       shift-x) 0]
                   [(- dx) (- height)]
                   [(+ half-width
                       (* -2 fimbriation-offset)) 0]
                   [dx height]
                   [(+ quarter-width
                       fimbriation-offset
                       (- shift-x)) 0]]))
     :offset  (if filled?
                (v/v 0 (- height))
                v/zero)}))

(defn urdy
  {:display-name "Urdy"}
  [{:keys [eccentricity
           height
           width]}
   _fimbriation-offset
   {:keys [reversed?] :as line-options}]
  (let [quarter-width (/ width 4)
        pointy-height (* quarter-width
                         (* 2)
                         (* (-> eccentricity
                                (* 0.6)
                                (+ 0.2)))
                         (* height))
        middle-height (* quarter-width height)
        half-height   (/ middle-height 2)]
    {:pattern (if reversed?
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
     :offset  v/zero}))

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
  {:type         {:type    :choice
                  :choices choices
                  :default :straight}
   :eccentricity {:type    :range
                  :min     0
                  :max     1
                  :default 0.5}
   :height       {:type    :range
                  :min     0.2
                  :max     3
                  :default 1}
   :width        {:type    :range
                  :min     2
                  :max     100
                  :default 10}
   :offset       {:type    :range
                  :min     -1
                  :max     3
                  :default 0}
   :flipped?     {:type    :boolean
                  :default false}
   :fimbriation  {:mode        {:type    :choice
                                :choices fimbriation-choices
                                :default :none}
                  :alignment   {:type    :choice
                                :choices fimbriation-alignment-choices
                                :default :even}
                  :outline?    {:type    :boolean
                                :default false}
                  :thickness-1 {:type    :range
                                :min     1
                                :max     10
                                :default 6}
                  :tincture-1  {:type    :choice
                                :choices (-> [["None" :none]]
                                             (into tincture/choices))
                                :default :none}
                  :thickness-2 {:type    :range
                                :min     1
                                :max     10
                                :default 3}
                  :tincture-2  {:type    :choice
                                :choices (-> [["None" :none]]
                                             (into tincture/choices))
                                :default :none}}})

(defn options [line]
  (options/merge
   default-options
   (get {:straight  {:eccentricity nil
                     :offset       nil
                     :height       nil
                     :width        nil
                     :flipped?     nil}
         :invected  {:eccentricity {:default 1}}
         :engrailed {:eccentricity {:default 1}}
         :indented  {:eccentricity nil}
         :embattled {:eccentricity nil}
         :dancetty  {:width        {:default 20}
                     :eccentricity nil}
         :wavy      {:width {:default 20}}}
        (:type line))))

(defn jiggle [[previous
               {:keys [x y] :as current}
               _]]
  (let [dist          (-> current
                          (v/- previous)
                          (v/abs))
        jiggle-radius (/ dist 4)
        dx            (- (* (random/float) jiggle-radius)
                         jiggle-radius)
        dy            (- (* (random/float) jiggle-radius)
                         jiggle-radius)]
    {:x (+ x dx)
     :y (+ y dy)}))

(defn squiggly-path [path & {:keys [seed]}]
  (random/seed (if seed
                 [seed path]
                 path))
  (let [points   (-> path
                     svg/new-path
                     (svg/points :length))
        points   (vec (concat [(first points)]
                              (map jiggle (partition 3 1 points))
                              [(last points)]))
        curve    (catmullrom/catmullrom points)
        new-path (catmullrom/curve->svg-path-relative curve)]
    new-path))

(defn squiggly-paths [data]
  (walk/postwalk #(cond-> %
                    (vector? %) ((fn [v]
                                   (if (= (first v) :d)
                                     [:d (squiggly-path (second v))]
                                     v))))
                 data))

(defn create [{:keys [type] :or {type :straight} :as line} length & {:keys [angle flipped? render-options seed] :as line-options}]
  (let [line-function          (get kinds-function-map type)
        line-options-values    (options/sanitize line (options line))
        line-data              (if (= line-function #'straight)
                                 (line-function line length line-options)
                                 (line-with-offset2
                                  line-options-values
                                  length
                                  line-function
                                  line-options))
        line-flipped?          (:flipped? line-options-values)
        adjusted-path          (-> line-data
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
                                           (:squiggly? render-options) (squiggly-path :seed [seed :fimbriation-2])))
        effective-flipped?     (or (and flipped? (not line-flipped?))
                                   (and (not flipped?) line-flipped?))]
    (-> line-data
        (assoc :line
               (-> adjusted-path
                   svgpath
                   (cond->
                       effective-flipped? (.scale 1 -1))
                   (.rotate angle)
                   .toString))
        (assoc :fimbriation-1
               (some-> adjusted-fimbriation-1
                       svgpath
                       (.scale -1 1)
                       (cond->
                           effective-flipped? (.scale 1 -1))
                       (.rotate angle)
                       .toString))
        (assoc :fimbriation-2
               (some-> adjusted-fimbriation-2
                       svgpath
                       (.scale -1 1)
                       (cond->
                           effective-flipped? (.scale 1 -1))
                       (.rotate angle)
                       .toString))
        (update :line-start (fn [p] (when p (v/rotate p angle))))
        (update :line-end (fn [p] (when p (v/rotate p angle))))
        (update :fimbriation-1-start (fn [p] (when p (v/rotate p angle))))
        (update :fimbriation-1-end (fn [p] (when p (v/rotate p angle))))
        (update :fimbriation-2-start (fn [p] (when p (v/rotate p angle))))
        (update :fimbriation-2-end (fn [p] (when p (v/rotate p angle)))))))

(defn translate [path dx dy]
  (-> path
      svgpath
      (.translate dx dy)
      .toString))

(defn stitch [path]
  ;; TODO: this can be improved, it already broke some things and caused unexpected behaviour,
  ;; because the 'e' was not part of the pattern
  (s/replace path #"^M[ ]*[0-9.e-]+[, -] *[0-9.e-]+" ""))
