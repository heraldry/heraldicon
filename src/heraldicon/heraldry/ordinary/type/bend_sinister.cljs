(ns heraldicon.heraldry.ordinary.type.bend-sinister
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]))

(def ordinary-type :heraldry.ordinary.type/bend-sinister)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/bend-sinister)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [parent-context (interface/parent context)
        {:keys [affected-paths]} (interface/get-auto-ordinary-info ordinary-type parent-context)
        auto-position-index (get affected-paths (:path context))
        auto-positioned? auto-position-index
        default-size (interface/get-sanitized-data (c/++ parent-context :bend-sinister-group :default-size))
        default-spacing (interface/get-sanitized-data (c/++ parent-context :bend-sinister-group :default-spacing))
        default-adapt-to-ordinaries? (interface/get-sanitized-data (c/++ parent-context :bend-sinister-group :adapt-to-ordinaries?))
        line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside)
                       (cond->
                         auto-positioned? (options/override-if-exists [:size-reference :default] :field-width)))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside)
                                (cond->
                                  auto-positioned? (options/override-if-exists [:size-reference :default] :field-width)))
        anchor-point-option {:type :option.type/choice
                             :choices (position/anchor-choices
                                       [:auto
                                        :fess
                                        :chief
                                        :base
                                        :honour
                                        :nombril
                                        :hoist
                                        :fly
                                        :top-right
                                        :center
                                        :bottom-left])
                             :default :auto
                             :ui/label :string.option/point}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)
        orientation-point-option (if auto-positioned?
                                   {:type :option.type/choice
                                    :choices (position/orientation-choices
                                              [:auto])
                                    :default :auto
                                    :ui/label :string.option/point}
                                   {:type :option.type/choice
                                    :choices (position/orientation-choices
                                              (case current-anchor-point
                                                :auto [:fess
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
                                               :auto :fess
                                               :top-right :fess
                                               :bottom-left :fess
                                               :top-right)
                                    :ui/label :string.option/point})
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    (ordinary.shared/add-humetty-and-voided
     {:adapt-to-ordinaries? {:type :option.type/boolean
                             :default (if auto-positioned?
                                        default-adapt-to-ordinaries?
                                        true)
                             :override (when auto-positioned?
                                         default-adapt-to-ordinaries?)
                             :ui/disabled? (boolean auto-positioned?)
                             :ui/label :string.option/adapt-to-ordinaries?}
      :anchor (cond-> {:point anchor-point-option
                       :ui/label :string.option/anchor
                       :ui/element :ui.element/position}
                (and auto-positioned?
                     (pos? auto-position-index)) (assoc :spacing-top {:type :option.type/range
                                                                      :min -75
                                                                      :max 75
                                                                      :default default-spacing
                                                                      :ui/label :string.option/spacing-top
                                                                      :ui/step 0.1})
                (not auto-positioned?) (assoc :alignment {:type :option.type/choice
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
      :orientation (cond-> {:point orientation-point-option
                            :ui/label :string.option/orientation
                            :ui/element :ui.element/position}

                     (and (not auto-positioned?)
                          (= current-orientation-point
                             :angle)) (assoc :angle {:type :option.type/range
                                                     :min 0
                                                     :max 360
                                                     :default 45
                                                     :ui/label :string.option/angle})

                     (and (not auto-positioned?)
                          (not= current-orientation-point
                                :angle)) (assoc :offset-x {:type :option.type/range
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
                                                           :ui/step 0.1})

                     (and (not auto-positioned?)
                          (not= current-orientation-point
                                :angle)) (assoc :alignment {:type :option.type/choice
                                                            :choices position/alignment-choices
                                                            :default :middle
                                                            :ui/label :string.option/alignment
                                                            :ui/element :ui.element/radio-select}))
      :line line-style
      :opposite-line opposite-line-style
      :geometry {:size {:type :option.type/range
                        :min 0.1
                        :max 90
                        :default (if auto-positioned?
                                   default-size
                                   25)
                        :ui/label :string.option/size
                        :ui/step 0.1}
                 :ui/label :string.option/geometry
                 :ui/element :ui.element/geometry}
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 2 :size-reference-default (when auto-positioned?
                                                                              :field-width))}
     context)))

(defmethod interface/auto-arrangement ordinary-type [real-ordinary-type context]
  ((get-method interface/auto-arrangement :heraldry.ordinary.type/bend) real-ordinary-type context))

(defmethod interface/properties ordinary-type [context]
  ((get-method interface/properties :heraldry.ordinary.type/bend) context))

(defmethod interface/environment ordinary-type [context properties]
  ((get-method interface/environment :heraldry.ordinary.type/bend) context properties))

(defmethod interface/bounding-box ordinary-type [context properties]
  ((get-method interface/bounding-box :heraldry.ordinary.type/bend) context properties))

(defmethod interface/render-shape ordinary-type [context properties]
  ((get-method interface/render-shape :heraldry.ordinary.type/bend) context properties))

(defmethod interface/exact-shape ordinary-type [context properties]
  ((get-method interface/exact-shape :heraldry.ordinary.type/bend) context properties))

(defmethod cottising/cottise-properties ordinary-type [context properties]
  ((get-method cottising/cottise-properties :heraldry.ordinary.type/bend) context properties))
