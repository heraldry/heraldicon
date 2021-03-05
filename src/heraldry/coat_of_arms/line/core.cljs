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

(defn line-with-offset [{pattern-width :width
                         line-offset :offset
                         flipped? :flipped?
                         :as line}
                        length line-function {:keys [joint-angle]
                                              :as line-options}]
  (let [base-line 0
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

        line-pattern (line-function line (if flipped?
                                           (- base-line)
                                           base-line) line-options)
        offset-length (* line-offset pattern-width)
        repetitions (-> length
                        (- offset-length)
                        (/ pattern-width)
                        Math/ceil
                        int
                        inc)
        line-start (v/v (min 0 offset-length) 0)]
    {:line (-> [["h" (cond-> 0
                       (pos? offset-length) (+ offset-length))]]
               (into (repeat repetitions line-pattern))
               (->> (apply merge))
               vec)
     :line-start (v/+ (v/v line-start-x
                           base-line)
                      line-start)
     :up (v/v 0 -50)
     :down (v/v 0 50)}))

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
                 :corner {:type :choice
                          :choices [["Round" :round]
                                    ["Sharp" :sharp]
                                    ["Bevel" :bevel]]
                          :default :sharp}
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
        line-up (:up line-data)
        line-down (:down line-data)
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

        line-flipped? (:flipped? line-options-values)
        effective-flipped? (or (and flipped? (not line-flipped?))
                               (and (not flipped?) line-flipped?))]
    (-> line-data
        (assoc :line
               (-> line-path
                   (cond->
                    (:squiggly? render-options) (svg/squiggly-path :seed seed))
                   svgpath
                   (cond->
                    effective-flipped? (.scale 1 -1))
                   (.rotate angle)
                   .toString))
        (assoc :line-start (when line-start (v/rotate line-start angle)))
        (assoc :line-end (when line-end (v/rotate line-end angle)))
        (assoc :up (when line-up (v/rotate line-up angle)))
        (assoc :down (when line-down (v/rotate line-down angle))))))
