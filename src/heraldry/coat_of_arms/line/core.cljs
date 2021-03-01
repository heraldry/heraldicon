(ns heraldry.coat-of-arms.line.core
  (:require ["svgpath" :as svgpath]
            [heraldry.coat-of-arms.line.type.dancetty :as dancetty]
            [heraldry.coat-of-arms.line.type.dovetailed :as dovetailed]
            [heraldry.coat-of-arms.line.type.embattled :as embattled]
            [heraldry.coat-of-arms.line.type.engrailed :as engrailed]
            [heraldry.coat-of-arms.line.type.indented :as indented]
            [heraldry.coat-of-arms.line.type.invected :as invected]
            [heraldry.coat-of-arms.line.type.raguly :as raguly]
            [heraldry.coat-of-arms.line.type.straight :as straight]
            [heraldry.coat-of-arms.line.type.urdy :as urdy]
            [heraldry.coat-of-arms.line.type.wavy :as wavy]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(declare options)

(defn line-with-offset [{fimbriation   :fimbriation
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

        line-pattern          (line-function line base-line line-options)
        fimbriation-1-pattern (line-function line fimbriation-1-line (-> line-options
                                                                         (update :reversed? not)))
        fimbriation-2-pattern (line-function line fimbriation-2-line (-> line-options
                                                                         (update :reversed? not)))
        offset-length         (* line-offset pattern-width)
        repetitions           (-> length
                                  (- offset-length)
                                  (/ pattern-width)
                                  Math/ceil
                                  int
                                  inc)
        actual-length         (-> (* repetitions pattern-width)
                                  (cond->
                                      (pos? offset-length) (+ offset-length)))
        line-start            (v/v (min 0 offset-length) 0)
        line-end              (-> (v/v actual-length 0)
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
                                                (not reversed?) (into [["h" (cond-> 0
                                                                              (pos? line-start-x)  (+ line-start-x)
                                                                              (pos? offset-length) (+ offset-length))]]))
                                            (into (repeat repetitions line-pattern))
                                            (cond->
                                                reversed? (into [["h" (cond-> 0
                                                                        (pos? line-start-x)  (+ line-start-x)
                                                                        (pos? offset-length) (+ offset-length))]]))
                                            (->> (apply merge))
                                            vec)
                            :line-start (v/+ (v/v (- line-start-x)
                                                  base-line)
                                             line-start)
                            :line-end   (v/+ (v/v (cond-> 0
                                                    (neg? line-start-x) (+ line-start-x))
                                                  base-line)
                                             line-end)}
        fimbriation-1-data (when (#{:single :double} fimbriation-mode)
                             {:fimbriation-1       (-> []
                                                       (cond->
                                                           reversed?  (into [["h" (cond-> 0
                                                                                    (pos? fimbriation-1-offset-x) (+ fimbriation-1-offset-x)
                                                                                    (pos? offset-length)          (+ offset-length))]]))
                                                       (into (repeat repetitions fimbriation-1-pattern))
                                                       (cond->
                                                           (not reversed?) (into [["h" (cond-> 0
                                                                                         (pos? fimbriation-1-offset-x) (+ fimbriation-1-offset-x)
                                                                                         (pos? offset-length)          (+ offset-length))]]))
                                                       (->> (apply merge))
                                                       vec)
                              :fimbriation-1-start (v/+ (v/v fimbriation-1-offset-x
                                                             fimbriation-1-line)
                                                        line-start)
                              :fimbriation-1-end   (v/+ (v/v (cond-> 0
                                                               (neg? fimbriation-1-offset-x) (- fimbriation-1-offset-x))
                                                             fimbriation-1-line)
                                                        line-end)})
        fimbriation-2-data (when (#{:double} fimbriation-mode)
                             {:fimbriation-2       (-> []
                                                       (cond->
                                                           reversed?  (into [["h" (cond-> 0
                                                                                    (pos? fimbriation-2-offset-x) (+ fimbriation-2-offset-x)
                                                                                    (pos? offset-length)          (+ offset-length))]]))
                                                       (into (repeat repetitions fimbriation-2-pattern))
                                                       (cond->
                                                           (not reversed?)  (into [["h" (cond-> 0
                                                                                          (pos? fimbriation-2-offset-x) (+ fimbriation-2-offset-x)
                                                                                          (pos? offset-length)          (+ offset-length))]]))
                                                       (->> (apply merge))
                                                       vec)
                              :fimbriation-2-start (v/+ (v/v fimbriation-2-offset-x
                                                             fimbriation-2-line)
                                                        line-start)
                              :fimbriation-2-end   (v/+ (v/v (cond-> 0
                                                               (neg? fimbriation-2-offset-x) (- fimbriation-2-offset-x))
                                                             fimbriation-2-line)
                                                        line-end)})
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

(def lines
  [#'straight/pattern
   #'invected/pattern
   #'engrailed/pattern
   #'embattled/pattern
   #'indented/pattern
   #'dancetty/pattern
   #'wavy/pattern
   #'dovetailed/pattern
   #'raguly/pattern
   #'urdy/pattern])

(def kinds-function-map
  (->> lines
       (map (fn [function]
              [(-> function meta :value) function]))
       (into {})))

(def choices
  (->> lines
       (map (fn [function]
              [(-> function meta :display-name) (-> function meta :value)]))))

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
         :indented  {:eccentricity nil}
         :embattled {:eccentricity nil}
         :dancetty  {:width        {:default 20}
                     :eccentricity nil}
         :wavy      {:width {:default 20}}}
        (:type line))))

(defn create [{:keys [type] :or {type :straight} :as line} length & {:keys [angle flipped? render-options seed] :as line-options}]
  (let [line-function          (get kinds-function-map type)
        line-options-values    (cond-> (options/sanitize line (options line))
                                 (= type :straight) (-> (assoc :width length)
                                                        (assoc :offset 0)))
        line-data              (line-with-offset
                                line-options-values
                                length
                                line-function
                                line-options)
        line-flipped?          (:flipped? line-options-values)
        adjusted-path          (-> line-data
                                   :line
                                   svg/make-path
                                   (->>
                                    (str "M 0,0 "))
                                   (cond->
                                       (:squiggly? render-options) (svg/squiggly-path :seed seed)))
        adjusted-fimbriation-1 (some-> line-data
                                       :fimbriation-1
                                       svg/make-path
                                       (->>
                                        (str "M 0,0 "))
                                       (cond->
                                           (:squiggly? render-options) (svg/squiggly-path :seed [seed :fimbriation-1])))
        adjusted-fimbriation-2 (some-> line-data
                                       :fimbriation-2
                                       svg/make-path
                                       (->>
                                        (str "M 0,0 "))
                                       (cond->
                                           (:squiggly? render-options) (svg/squiggly-path :seed [seed :fimbriation-2])))
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
