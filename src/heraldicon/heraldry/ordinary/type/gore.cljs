(ns heraldicon.heraldry.ordinary.type.gore
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.shared :as field.shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(defn- arm-diagonal [anchor-point orientation-point]
  (-> (v/sub orientation-point anchor-point)
      v/normal
      (v/mul 200)))

(def ordinary-type :heraldry.ordinary.type/gore)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/gore)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))
        orientation-point-option {:type :choice
                                  :choices (position/orientation-choices
                                            [:top-left
                                             :top-right
                                             :angle])
                                  :default :top-left
                                  :ui {:label :string.option/point}}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    (ordinary.shared/add-humetty-and-voided
     {:anchor {:point {:type :choice
                       :choices (position/anchor-choices
                                 [:fess
                                  :chief
                                  :base
                                  :center])
                       :default :fess
                       :ui {:label :string.option/point}}
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
               :ui {:label :string.option/anchor
                    :form-type :position}}
      :orientation (cond-> {:point orientation-point-option
                            :ui {:label :string.option/orientation
                                 :form-type :position}}

                     (= current-orientation-point
                        :angle) (assoc :angle {:type :range
                                               :min -80
                                               :max 80
                                               :default -45
                                               :ui {:label :string.option/angle}})

                     (not= current-orientation-point
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
                                                          :step 0.1}}))
      :line line-style
      :opposite-line opposite-line-style
      :outline? options/plain-outline?-option} context)))

(defmethod ordinary.interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))

        points (:points environment)
        width (:width environment)
        top-left (:top-left points)
        top-right (:top-right points)
        left? (case (:point orientation)
                :top-left true
                :angle (-> orientation :angle neg?)
                false)
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               environment
                                               anchor
                                               orientation
                                               0
                                               -90)
        bottom (:bottom points)
        relative-arm (arm-diagonal anchor-point orientation-point)
        diagonal-top (v/add anchor-point relative-arm)
        [_ intersection-top] (v/environment-intersections anchor-point diagonal-top environment)
        flipped? (not left?)
        {line-diagonal :line
         line-diagonal-start :line-start
         :as line-diagonal-data} (line/create line
                                              anchor-point diagonal-top
                                              :real-start 0
                                              :real-end (v/abs (v/sub intersection-top anchor-point))
                                              :flipped? flipped?
                                              :reversed? true
                                              :context context
                                              :environment environment)
        {line-down :line
         line-down-end :line-end
         :as line-down-data} (line/create opposite-line
                                          anchor-point bottom
                                          :flipped? flipped?
                                          :real-start 0
                                          :real-end (v/abs (v/sub bottom anchor-point))
                                          :context context
                                          :environment environment)
        shape (ordinary.shared/adjust-shape
               ["M" (v/add diagonal-top
                           line-diagonal-start)
                (path/stitch line-diagonal)
                "L" anchor-point
                (path/stitch line-down)
                (infinity/path (if left?
                                 :clockwise
                                 :counter-clockwise)
                               [:bottom :top]
                               [(v/add bottom
                                       line-down-end)
                                (v/add diagonal-top
                                       line-diagonal-start)])
                "z"]
               width
               (/ width 2)
               context)
        part [shape
              [(if left?
                 top-left
                 top-right)
               anchor-point
               bottom]]]
    [:<>
     [field.shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (ordinary.shared/adjusted-shape-outline
      shape outline? context
      [line/render line [line-diagonal-data line-down-data] diagonal-top outline? context])]))
