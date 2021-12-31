(ns heraldry.coat-of-arms.field.type.per-pile
  (:require
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.field.shared :as shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.coat-of-arms.shared.pile :as pile]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]
   [heraldry.util :as util]))

(def field-type :heraldry.field.type/per-pile)

(defmethod field-interface/display-name field-type [_] :string.field.type/per-pile)

(defmethod field-interface/part-names field-type [_] nil)

(defmethod interface/options field-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil))
        origin-point-option {:type :choice
                             :choices [[:string.option.point-choice/chief :chief]
                                       [:string.option.point-choice/base :base]
                                       [:string.option.point-choice/dexter :dexter]
                                       [:string.option.point-choice/sinister :sinister]
                                       [:string.option.point-choice/top-left :top-left]
                                       [:string.option.point-choice/top :top]
                                       [:string.option.point-choice/top-right :top-right]
                                       [:string.option.point-choice/left :left]
                                       [:string.option.point-choice/right :right]
                                       [:string.option.point-choice/bottom-left :bottom-left]
                                       [:string.option.point-choice/bottom :bottom]
                                       [:string.option.point-choice/bottom-right :bottom-right]]
                             :default :bottom
                             :ui {:label :string.option/point}}
        current-origin-point (options/get-value
                              (interface/get-raw-data (c/++ context :origin :point))
                              origin-point-option)
        anchor-point-option {:type :choice
                             :choices (util/filter-choices
                                       [[:string.option.point-choice/top-left :top-left]
                                        [:string.option.point-choice/top :top]
                                        [:string.option.point-choice/top-right :top-right]
                                        [:string.option.point-choice/left :left]
                                        [:string.option.point-choice/right :right]
                                        [:string.option.point-choice/bottom-left :bottom-left]
                                        [:string.option.point-choice/bottom :bottom]
                                        [:string.option.point-choice/bottom-right :bottom-right]
                                        [:string.option.point-choice/fess :fess]
                                        [:string.option.point-choice/chief :chief]
                                        [:string.option.point-choice/base :base]
                                        [:string.option.point-choice/dexter :dexter]
                                        [:string.option.point-choice/sinister :sinister]
                                        [:string.option.point-choice/honour :honour]
                                        [:string.option.point-choice/nombril :nombril]
                                        [:string.option.anchor-point-choice/angle :angle]]
                                       #(not= % current-origin-point))
                             :default :fess
                             :ui {:label :string.option/point}}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)
        size-mode-option {:type :choice
                          :choices [[:string.option.size-mode-choice/thickness :thickness]
                                    [:string.option.size-mode-choice/angle :angle]]
                          :default :thickness
                          :ui {:label :string.option/size-mode
                               :form-type :radio-select}}
        current-size-mode (options/get-value
                           (interface/get-raw-data (c/++ context :geometry :size-mode))
                           size-mode-option)]
    {:origin {:point origin-point-option
              :alignment {:type :choice
                          :choices position/alignment-choices
                          :default :middle
                          :ui {:label :string.option/alignment
                               :form-type :radio-select}}
              :offset-x {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label :string.option/offset-x
                              :step 0.1}}
              :offset-y {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label :string.option/offset-y
                              :step 0.1}}
              :ui {:label :string.option/origin
                   :form-type :position}}
     :anchor (cond-> {:point anchor-point-option
                      :ui {:label :string.option/anchor
                           :form-type :position}}

               (= current-anchor-point
                  :angle) (assoc :angle {:type :range
                                         :min (cond
                                                (#{:top-left
                                                   :top-right
                                                   :bottom-left
                                                   :bottom-right} current-origin-point) 0
                                                :else -90)
                                         :max 90
                                         :default (cond
                                                    (#{:top-left
                                                       :top-right
                                                       :bottom-left
                                                       :bottom-right} current-origin-point) 45
                                                    :else 0)
                                         :ui {:label :string.option/angle}})

               (not= current-anchor-point
                     :angle) (assoc :offset-x {:type :range
                                               :min -45
                                               :max 45
                                               :default 0
                                               :ui {:label :string.option/offset-x
                                                    :step 0.1}}
                                    :offset-y {:type :range
                                               :min -45
                                               :max 45
                                               :default 0
                                               :ui {:label :string.option/offset-y
                                                    :step 0.1}}
                                    :type {:type :choice
                                           :choices [[:string.option.anchor-type-choice/edge :edge]
                                                     [:string.option.anchor-type-choice/anchor-point :point]]
                                           :default :edge
                                           :ui {:label :string.render-options/mode
                                                :form-type :radio-select}}))
     :line line-style
     :opposite-line opposite-line-style
     :geometry {:size-mode size-mode-option
                :size {:type :range
                       :min 5
                       :max 120
                       :default (case current-size-mode
                                  :thickness 75
                                  30)
                       :ui {:label :string.option/size
                            :step 0.1}}
                :stretch {:type :range
                          :min 0.33
                          :max 2
                          :default 1
                          :ui {:label :string.option/stretch
                               :step 0.01}}
                :ui {:label :string.option/geometry
                     :form-type :geometry}}}))

(defmethod field-interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        origin (interface/get-sanitized-data (c/++ context :origin))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        geometry (interface/get-sanitized-data (c/++ context :geometry))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        anchor (-> anchor
                   (assoc :type :edge))
        geometry (-> geometry
                     (assoc :stretch 1))
        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        thickness-base (if (#{:left :right} (:point origin))
                         (:height environment)
                         (:width environment))
        {origin-point :origin
         point :point
         thickness :thickness} (pile/calculate-properties
                                environment
                                origin
                                (cond-> anchor
                                  (#{:top-right
                                     :right
                                     :bottom-left} (:point origin)) (update :angle #(when %
                                                                                      (- %))))
                                geometry
                                thickness-base
                                (case (:point origin)
                                  :top-left 0
                                  :top 90
                                  :top-right 180
                                  :left 0
                                  :right 180
                                  :bottom-left 0
                                  :bottom -90
                                  :bottom-right 180
                                  0))
        {left-point :left
         right-point :right} (pile/diagonals origin-point point thickness)
        intersection-left (-> (v/environment-intersections point left-point environment)
                              last)
        intersection-right (-> (v/environment-intersections point right-point environment)
                               last)
        end-left (-> intersection-left
                     (v/sub point)
                     v/abs)
        end-right (-> intersection-right
                      (v/sub point)
                      v/abs)
        end (max end-left end-right)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of thickness-base))
                 (update-in [:fimbriation :thickness-2] (util/percent-of thickness-base)))
        {line-left :line
         line-left-start :line-start
         line-left-end :line-end
         :as line-left-data} (line/create line
                                          point left-point
                                          :reversed? true
                                          :real-start 0
                                          :real-end end
                                          :context context
                                          :environment environment)
        {line-right :line
         line-right-start :line-start
         line-right-end :line-end
         :as line-right-data} (line/create opposite-line
                                           point right-point
                                           :real-start 0
                                           :real-end end
                                           :context context
                                           :environment environment)
        parts [[["M" (v/add point
                            line-right-start)
                 (path/stitch line-right)
                 (infinity/path
                  :counter-clockwise
                  (cond
                    (#{:top-left
                       :top
                       :top-right} (:point origin)) [:top :bottom]
                    (#{:left} (:point origin)) [:left :right]
                    (#{:right} (:point origin)) [:right :left]
                    (#{:bottom-left
                       :bottom
                       :bottom-right} (:point origin)) [:bottom :top]
                    :else [:top :bottom])
                  [(v/add point
                          line-right-end)
                   (v/add point
                          line-right-start)])
                 "z"]
                ;; TODO: these fields inherit the whole parent
                ;; environment points, but it can probably be reduced
                [top-left top-right
                 bottom-left bottom-right]]

               [["M" (v/add left-point
                            line-left-start)
                 (path/stitch line-left)
                 (path/stitch line-right)
                 "z"]
                ;; TODO: these fields inherit the whole parent
                ;; environment points, but it can probably be reduced
                [top-left top-right
                 bottom-left bottom-right]]

               [["M" (v/add left-point
                            line-left-start)
                 (path/stitch line-left)
                 (infinity/path
                  :counter-clockwise
                  (cond
                    (#{:top-left
                       :top
                       :top-right} (:point origin)) [:bottom :top]
                    (#{:left} (:point origin)) [:right :left]
                    (#{:right} (:point origin)) [:left :right]
                    (#{:bottom-left
                       :bottom
                       :bottom-right} (:point origin)) [:top :bottom]
                    :else [:bottom :top])
                  [(v/add left-point
                          line-left-end)
                   (v/add left-point
                          line-left-start)])
                 "z"]
                ;; TODO: these fields inherit the whole parent
                ;; environment points, but it can probably be reduced
                [top-left top-right
                 bottom-left bottom-right]]]]
    [:<>
     [shared/make-subfields
      context parts
      [:all nil nil]
      environment]
     [line/render line [line-left-data
                        line-right-data] left-point outline? context]]))
