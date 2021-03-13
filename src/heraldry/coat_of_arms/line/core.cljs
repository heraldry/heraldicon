(ns heraldry.coat-of-arms.line.core
  (:require ["svgpath" :as svgpath]
            [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
            [heraldry.coat-of-arms.line.type.dancetty :as dancetty]
            [heraldry.coat-of-arms.line.type.dovetailed :as dovetailed]
            [heraldry.coat-of-arms.line.type.embattled :as embattled]
            [heraldry.coat-of-arms.line.type.embattled-in-crosses :as embattled-in-crosses]
            [heraldry.coat-of-arms.line.type.engrailed :as engrailed]
            [heraldry.coat-of-arms.line.type.fir-tree-topped :as fir-tree-topped]
            [heraldry.coat-of-arms.line.type.fir-twigged :as fir-twigged]
            [heraldry.coat-of-arms.line.type.indented :as indented]
            [heraldry.coat-of-arms.line.type.invected :as invected]
            [heraldry.coat-of-arms.line.type.nebuly :as nebuly]
            [heraldry.coat-of-arms.line.type.potenty :as potenty]
            [heraldry.coat-of-arms.line.type.raguly :as raguly]
            [heraldry.coat-of-arms.line.type.straight :as straight]
            [heraldry.coat-of-arms.line.type.urdy :as urdy]
            [heraldry.coat-of-arms.line.type.wavy :as wavy]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn line-with-offset [{pattern-width :width
                         line-offset :offset
                         :as line}
                        length line-function line-options]
  (let [line-pattern (line-function line line-options)
        offset-length (* line-offset pattern-width)
        repetitions (-> length
                        (- offset-length)
                        (/ pattern-width)
                        Math/ceil
                        int
                        inc)
        line-start (v/v (min 0 offset-length) 0)]
    {:line (-> []
               (cond->
                (pos? offset-length) (into [["h" offset-length]]))
               (into (repeat repetitions line-pattern))
               (->> (apply merge))
               vec)
     :line-start line-start
     :up (v/v 0 -50)
     :down (v/v 0 50)}))

(def lines
  [#'straight/pattern
   #'invected/pattern
   #'engrailed/pattern
   #'embattled/pattern
   #'embattled-in-crosses/pattern
   #'potenty/pattern
   #'indented/pattern
   #'dancetty/pattern
   #'wavy/pattern
   #'dovetailed/pattern
   #'raguly/pattern
   #'urdy/pattern
   #'nebuly/pattern
   #'fir-tree-topped/pattern
   #'fir-twigged/pattern])

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
  (when-let [type (:type line)]
    (case type
      :straight (options/pick default-options
                              [[:type]
                               [:fimbriation]])
      :indented (options/pick default-options
                              [[:type]
                               [:height]
                               [:width]
                               [:offset]
                               [:flipped?]
                               [:fimbriation]])
      :embattled (options/pick default-options
                               [[:type]
                                [:height]
                                [:width]
                                [:offset]
                                [:flipped?]
                                [:fimbriation]])
      :dancetty (options/pick default-options
                              [[:type]
                               [:height]
                               [:width]
                               [:offset]
                               [:flipped?]
                               [:fimbriation]]
                              {[:width :default] 20})
      :wavy (options/pick default-options
                          [[:type]
                           [:eccentricity]
                           [:height]
                           [:width]
                           [:offset]
                           [:flipped?]
                           [:fimbriation]]
                          {[:width :default] 20})
      :fir-twigged (options/pick default-options
                                 [[:type]
                                  [:height]
                                  [:width]
                                  [:offset]
                                  [:flipped?]
                                  [:fimbriation]])
      (options/pick default-options
                    [[:type]
                     [:eccentricity]
                     [:height]
                     [:width]
                     [:offset]
                     [:flipped?]
                     [:fimbriation]]))))

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
        (assoc :line-end (when line-end (v/rotate (v/+ base-end line-end) angle)))
        (assoc :up (when line-up (v/rotate line-up angle)))
        (assoc :down (when line-down (v/rotate line-down angle))))))

(defn mask-intersection-points [start line-datas direction]
  (->> line-datas
       (map (fn [{:keys [line-end up down]}]
              (let [dv (case direction
                         :up up
                         down)]
                {:start-offset dv
                 :end-offset (v/+ line-end dv)})))

       (reduce (fn [current {:keys [start-offset end-offset]}]
                 (if (empty? current)
                   [{:start (v/+ start start-offset)
                     :end (v/+ start end-offset)}]
                   (let [{previous-end :end} (last current)]
                     (conj current {:start (v/+ previous-end start-offset)
                                    :end (v/+ previous-end end-offset)})))) [])
       (partition 2 1)
       (map (fn [[{start1 :start end1 :end}
                  {start2 :start end2 :end}]]
              (v/line-intersection start1 end1 start2 end2)))))

(defn render [line line-datas start outline? render-options]
  (let [{:keys [fimbriation]} line
        line-start (-> line-datas first :line-start)
        base-path ["M" (v/+ start line-start)
                   (for [{line-path-snippet :line} line-datas]
                     (svg/stitch line-path-snippet))]
        line-path (svg/make-path base-path)
        {:keys [mode
                alignment
                thickness-1
                thickness-2
                tincture-1
                tincture-2
                corner]} fimbriation
        [thickness-1 thickness-2
         tincture-1 tincture-2] (if (and (= alignment :inside)
                                         (= mode :double))
                                  [thickness-2 thickness-1
                                   tincture-2 tincture-1]
                                  [thickness-1 thickness-2
                                   tincture-1 tincture-2])
        mask-shape-top (when (#{:even :outside} alignment)
                         (let [mask-points (-> [(v/+ start line-start (-> line-datas first :up))]
                                               (into (mask-intersection-points start line-datas :up)))]
                           (svg/make-path [base-path
                                           "l" (-> line-datas last :up)
                                           (for [mask-point (reverse mask-points)]
                                             ["L" mask-point])
                                           "z"])))
        mask-shape-bottom (when (#{:even :inside} alignment)
                            (let [mask-points (-> [(v/+ start line-start (-> line-datas first :down))]
                                                  (into (mask-intersection-points start line-datas :down)))]
                              (svg/make-path [base-path
                                              "l" (-> line-datas last :down)
                                              (for [mask-point (reverse mask-points)]
                                                ["L" mask-point])
                                              "z"])))
        combined-thickness (+ thickness-1 thickness-2)
        mask-id-top (when mask-shape-top
                      (util/id "mask-line-top"))
        mask-id-bottom (when mask-shape-bottom
                         (util/id "mask-line-bottom"))]
    [:<>
     (when (#{:single :double} mode)
       [:<>
        (when (or mask-shape-top mask-shape-bottom)
          [:defs
           (when mask-shape-top
             [:mask {:id mask-id-top}
              [:path {:d mask-shape-top
                      :fill "#ffffff"}]])
           (when mask-shape-bottom
             [:mask {:id mask-id-bottom}
              [:path {:d mask-shape-bottom
                      :fill "#ffffff"}]])])
        (if (= alignment :even)
          [:<>
           (if (= mode :single)
             [fimbriation/render line-path nil (/ thickness-1 2)
              (tincture/pick tincture-1 render-options)
              outline? corner render-options]
             (cond
               (> thickness-1 thickness-2) [:<>
                                            [fimbriation/render line-path nil (/ combined-thickness 2)
                                             (tincture/pick tincture-2 render-options)
                                             outline? corner render-options]
                                            [fimbriation/render line-path nil (-> combined-thickness
                                                                                  (/ 2)
                                                                                  (- thickness-2))
                                             (tincture/pick tincture-1 render-options)
                                             outline? corner render-options]
                                            [fimbriation/render line-path mask-id-bottom
                                             (cond-> (/ combined-thickness 2)
                                               outline? (- outline/stroke-width))
                                             (tincture/pick tincture-1 render-options)
                                             false corner render-options]]
               (= thickness-1 thickness-2) [:<>
                                            [fimbriation/render line-path mask-id-top (/ combined-thickness 2)
                                             (tincture/pick tincture-2 render-options)
                                             outline? corner render-options]
                                            [fimbriation/render line-path mask-id-bottom (/ combined-thickness 2)
                                             (tincture/pick tincture-1 render-options)
                                             outline? corner render-options]
                                            (when outline?
                                              [fimbriation/render line-path mask-id-bottom 0
                                               nil
                                               outline? corner render-options])]
               (< thickness-1 thickness-2) [:<>
                                            [fimbriation/render line-path nil (/ combined-thickness 2)
                                             (tincture/pick tincture-1 render-options)
                                             outline? corner render-options]
                                            [fimbriation/render line-path nil (-> combined-thickness
                                                                                  (/ 2)
                                                                                  (- thickness-1))
                                             (tincture/pick tincture-2 render-options)
                                             outline? corner render-options]
                                            [fimbriation/render line-path mask-id-top
                                             (cond-> (/ combined-thickness 2)
                                               outline? (- outline/stroke-width))
                                             (tincture/pick tincture-2 render-options)
                                             false corner render-options]]))]
          [:g {:mask (case alignment
                       :outside (str "url(#" mask-id-top ")")
                       :inside (str "url(#" mask-id-bottom ")")
                       nil)}
           (when (#{:double} mode)
             [fimbriation/render line-path nil combined-thickness
              (tincture/pick tincture-2 render-options)
              outline? corner render-options])
           (when (#{:single :double} mode)
             [fimbriation/render line-path nil thickness-1
              (tincture/pick tincture-1 render-options)
              outline? corner render-options])])])
     (when (and outline?
                (not (and (= alignment :even)
                          (#{:single :double} mode))))
       [:g outline/style
        [:path {:d line-path}]])]))
