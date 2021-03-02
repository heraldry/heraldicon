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

(defn line-with-offset [{fimbriation :fimbriation
                         pattern-width :width
                         line-offset :offset
                         flipped? :flipped?
                         :as line}
                        length line-function {:keys [joint-angle]
                                              :as line-options}]
  (let [{fimbriation-mode :mode
         fimbriation-alignment :alignment
         fimbriation-thickness-1 :thickness-1
         fimbriation-thickness-2 :thickness-2} fimbriation

        fimbriation-thickness-1 (when (#{:single :double} fimbriation-mode)
                                  fimbriation-thickness-1)
        fimbriation-thickness-2 (when (#{:double} fimbriation-mode)
                                  fimbriation-thickness-2)
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
                    :else 0)
        fimbriation-1-line (- base-line
                              fimbriation-thickness-1)
        fimbriation-2-line (- base-line
                              fimbriation-thickness-1
                              fimbriation-thickness-2)
        offset-x-factor (if joint-angle
                          (-> joint-angle
                              (/ 2)
                              (* Math/PI)
                              (/ 180)
                              Math/tan
                              (->> (/ 1))
                              -)
                          0)
        line-start-x (* base-line
                        offset-x-factor)
        fimbriation-1-offset-x (* fimbriation-1-line
                                  offset-x-factor)
        fimbriation-2-offset-x (* fimbriation-2-line
                                  offset-x-factor)

        max-x-offset (max line-start-x fimbriation-1-offset-x fimbriation-2-offset-x)
        line-fill-x (- max-x-offset line-start-x)
        fimbriation-1-fill-x (- max-x-offset fimbriation-1-offset-x)
        fimbriation-2-fill-x (- max-x-offset fimbriation-2-offset-x)

        line-pattern (line-function line (if flipped?
                                           (- base-line)
                                           base-line) line-options)
        fimbriation-1-pattern (line-function line (if flipped?
                                                    (- fimbriation-1-line)
                                                    fimbriation-1-line) line-options)
        fimbriation-2-pattern (line-function line (if flipped?
                                                    (- fimbriation-2-line)
                                                    fimbriation-2-line) line-options)
        offset-length (* line-offset pattern-width)
        repetitions (-> length
                        (- offset-length)
                        (/ pattern-width)
                        Math/ceil
                        int
                        inc)
        line-start (v/v (min 0 offset-length) 0)

        line-data {:line (-> [["h" (cond-> line-fill-x
                                     (pos? offset-length) (+ offset-length))]]
                             (into (repeat repetitions line-pattern))
                             (->> (apply merge))
                             vec)
                   :line-start (v/+ (v/v line-start-x
                                         base-line)
                                    line-start)}
        fimbriation-1-data (when (#{:single :double} fimbriation-mode)
                             {:fimbriation-1 (-> [["h" (cond-> fimbriation-1-fill-x
                                                         (pos? offset-length) (+ offset-length))]]
                                                 (into (repeat repetitions fimbriation-1-pattern))
                                                 (->> (apply merge))
                                                 vec)
                              :fimbriation-1-start (v/+ (v/v fimbriation-1-offset-x
                                                             fimbriation-1-line)
                                                        line-start)})
        fimbriation-2-data (when (#{:double} fimbriation-mode)
                             {:fimbriation-2 (-> [["h" (cond-> fimbriation-2-fill-x
                                                         (pos? offset-length) (+ offset-length))]]
                                                 (into (repeat repetitions fimbriation-2-pattern))
                                                 (->> (apply merge))
                                                 vec)
                              :fimbriation-2-start (v/+ (v/v fimbriation-2-offset-x
                                                             fimbriation-2-line)
                                                        line-start)})]

    (merge line-data
           fimbriation-1-data
           fimbriation-2-data)))

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
         :indented {:eccentricity nil}
         :embattled {:eccentricity nil}
         :dancetty {:width {:default 20}
                    :eccentricity nil}
         :wavy {:width {:default 20}}}
        (:type line))))

(defn create [{:keys [type] :or {type :straight} :as line} length & {:keys [angle flipped? render-options seed reversed?] :as line-options}]
  (let [line-function (get kinds-function-map type)
        line-options-values (cond-> (options/sanitize line (options line))
                              (= type :straight) (-> (assoc :width length)
                                                     (assoc :offset 0)))
        base-end (v/v length 0)
        line-data (line-with-offset
                   line-options-values
                   length
                   line-function
                   line-options)
        line-path (-> line-data
                      :line
                      (->> (into ["M" 0 0]))
                      svg/make-path)
        {line-reversed-start :start
         line-reversed :path} (-> line-path
                                  svg/reverse-path)
        line-start (:line-start line-data)
        line-end (-> line-start
                     (v/+ line-reversed-start)
                     (v/- base-end))
        line-path (if reversed?
                    (-> line-reversed
                        svgpath
                        (.scale -1 1)
                        .toString)
                    line-path)
        [line-start line-end] (if reversed?
                                [(v/dot line-end (v/v -1 1))
                                 (v/dot line-start (v/v -1 1))]
                                [line-start line-end])

        fimbriation-1-path (some-> line-data
                                   :fimbriation-1
                                   (->> (into ["M" 0 0]))
                                   svg/make-path)
        {fimbriation-1-reversed-start :start
         fimbriation-1-reversed :path} (some-> fimbriation-1-path
                                               svg/reverse-path)
        fimbriation-1-start (:fimbriation-1-start line-data)
        fimbriation-1-end (some-> fimbriation-1-start
                                  (v/+ fimbriation-1-reversed-start)
                                  (v/- base-end))
        fimbriation-1-path (if (not reversed?)
                             (some-> fimbriation-1-reversed
                                     svgpath
                                     (.scale -1 1)
                                     .toString)
                             fimbriation-1-path)
        [fimbriation-1-start fimbriation-1-end] (if reversed?
                                                  [(v/dot fimbriation-1-end (v/v -1 1))
                                                   (v/dot fimbriation-1-start (v/v -1 1))]
                                                  [fimbriation-1-start fimbriation-1-end])

        fimbriation-2-path (some-> line-data
                                   :fimbriation-2
                                   (->> (into ["M" 0 0]))
                                   svg/make-path)
        {fimbriation-2-reversed-start :start
         fimbriation-2-reversed :path} (some-> fimbriation-2-path
                                               svg/reverse-path)
        fimbriation-2-start (:fimbriation-2-start line-data)
        fimbriation-2-end (some-> fimbriation-2-start
                                  (v/+ fimbriation-2-reversed-start)
                                  (v/- base-end))
        fimbriation-2-path (if (not reversed?)
                             (some-> fimbriation-2-reversed
                                     svgpath
                                     (.scale -1 1)
                                     .toString)
                             fimbriation-2-path)
        [fimbriation-2-start fimbriation-2-end] (if reversed?
                                                  [(v/dot fimbriation-2-end (v/v -1 1))
                                                   (v/dot fimbriation-2-start (v/v -1 1))]
                                                  [fimbriation-2-start fimbriation-2-end])

        line-flipped? (:flipped? line-options-values)
        effective-flipped? (or (and flipped? (not line-flipped?))
                               (and (not flipped?) line-flipped?))]
    (-> line-data
        (assoc :line
               (-> line-path
                   svgpath
                   (cond->
                    effective-flipped? (.scale 1 -1)
                    (:squiggly? render-options) (svg/squiggly-path :seed seed))
                   (.rotate angle)
                   .toString))
        (assoc :fimbriation-1
               (some-> fimbriation-1-path
                       svgpath
                       (.scale -1 1)
                       (cond->
                        effective-flipped? (.scale 1 -1)
                        (:squiggly? render-options) (svg/squiggly-path :seed [seed :fimbriation-1]))
                       (.rotate angle)
                       .toString))
        (assoc :fimbriation-2
               (some-> fimbriation-2-path
                       svgpath
                       (.scale -1 1)
                       (cond->
                        effective-flipped? (.scale 1 -1)
                        (:squiggly? render-options) (svg/squiggly-path :seed [seed :fimbriation-2]))
                       (.rotate angle)
                       .toString))
        (assoc :line-start (when line-start (v/rotate line-start angle)))
        (assoc :line-end (when line-end (v/rotate line-end angle)))
        (assoc :fimbriation-1-start (when fimbriation-1-start (v/rotate fimbriation-1-start angle)))
        (assoc :fimbriation-1-end (when fimbriation-1-end (v/rotate fimbriation-1-end angle)))
        (assoc :fimbriation-2-start (when fimbriation-2-start (v/rotate fimbriation-2-start angle)))
        (assoc :fimbriation-2-end (when fimbriation-2-end (v/rotate fimbriation-2-end angle))))))
