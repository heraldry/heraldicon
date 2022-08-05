(ns heraldicon.heraldry.ordinary.type.bend-sinister
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.render :as ordinary.render]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]))

(def ordinary-type :heraldry.ordinary.type/bend-sinister)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/bend-sinister)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))
        anchor-point-option {:type :option.type/choice
                             :choices (position/anchor-choices
                                       [:fess
                                        :chief
                                        :base
                                        :honour
                                        :nombril
                                        :hoist
                                        :fly
                                        :top-right
                                        :center
                                        :bottom-left])
                             :default :top-right
                             :ui/label :string.option/point}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            (case current-anchor-point
                                              :top-right [:fess
                                                          :center
                                                          :chief
                                                          :base
                                                          :honour
                                                          :nombril
                                                          :hoist
                                                          :fly
                                                          :bottom-left
                                                          :center
                                                          :angle]
                                              :bottom-left [:fess
                                                            :chief
                                                            :base
                                                            :honour
                                                            :nombril
                                                            :hoist
                                                            :fly
                                                            :top-right
                                                            :center
                                                            :angle]
                                              [:top-right
                                               :bottom-left
                                               :angle]))
                                  :default (case current-anchor-point
                                             :top-right :fess
                                             :bottom-left :fess
                                             :top-right)
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    (ordinary.shared/add-humetty-and-voided
     {:anchor {:point anchor-point-option
               :alignment {:type :option.type/choice
                           :choices position/alignment-choices
                           :default :middle
                           :ui/label :string.option/alignment
                           :ui/element :ui.element/radio-select}
               :offset-x {:type :option.type/range
                          :min -75
                          :max 75
                          :default 0
                          :ui/label :string.option/offset-x
                          :ui/step 0.1}
               :offset-y {:type :option.type/range
                          :min -75
                          :max 75
                          :default 0
                          :ui/label :string.option/offset-y
                          :ui/step 0.1}
               :ui/label :string.option/anchor
               :ui/element :ui.element/position}
      :orientation (cond-> {:point orientation-point-option
                            :ui/label :string.option/orientation
                            :ui/element :ui.element/position}

                     (= current-orientation-point
                        :angle) (assoc :angle {:type :option.type/range
                                               :min 0
                                               :max 360
                                               :default 45
                                               :ui/label :string.option/angle})

                     (not= current-orientation-point
                           :angle) (assoc :alignment {:type :option.type/choice
                                                      :choices position/alignment-choices
                                                      :default :middle
                                                      :ui/label :string.option/alignment
                                                      :ui/element :ui.element/radio-select}
                                          :offset-x {:type :option.type/range
                                                     :min -75
                                                     :max 75
                                                     :default 0
                                                     :ui/label :string.option/offset-x
                                                     :ui/step 0.1}
                                          :offset-y {:type :option.type/range
                                                     :min -75
                                                     :max 75
                                                     :default 0
                                                     :ui/label :string.option/offset-y
                                                     :ui/step 0.1}))
      :line line-style
      :opposite-line opposite-line-style
      :geometry {:size {:type :option.type/range
                        :min 0.1
                        :max 90
                        :default 25
                        :ui/label :string.option/size
                        :ui/step 0.1}
                 :ui/label :string.option/geometry
                 :ui/element :ui.element/geometry}
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 2)} context)))

(defmethod interface/properties ordinary-type [context]
  ((get-method interface/properties :heraldry.ordinary.type/bend) context))

(defmethod interface/environment ordinary-type [context properties]
  ((get-method interface/environment :heraldry.ordinary.type/bend) context properties))

(defmethod interface/render-shape ordinary-type [context properties]
  ((get-method interface/render-shape :heraldry.ordinary.type/bend) context properties))

(defmethod interface/exact-shape ordinary-type [context properties]
  ((get-method interface/exact-shape :heraldry.ordinary.type/bend) context properties))

(defmethod ordinary.interface/render-ordinary ordinary-type [context]
  (ordinary.render/render context))

(defmethod cottising/cottise-properties ordinary-type [context properties]
  ((get-method cottising/cottise-properties :heraldry.ordinary.type/bend) context properties))
