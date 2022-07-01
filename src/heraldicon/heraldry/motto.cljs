(ns heraldicon.heraldry.motto
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ribbon :as ribbon]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]))

;; TODO: this appears to be broken
(def tinctures-without-furs
  (-> tincture/choices
      (update 0 #(filterv (fn [v]
                            (or (not (vector? v))
                                (-> v second (not= :none))))
                          %))
      (->> (filterv #(when (-> % first :en (not= "Fur")) %)))))

(def tinctures-without-furs-map
  (options/choices->map tinctures-without-furs))

(derive :heraldry.motto.type/motto :heraldry.motto/type)
(derive :heraldry.motto.type/slogan :heraldry.motto/type)
(derive :heraldry.motto/type :heraldry/motto)

(def ^:private type-choices
  [[:string.entity/motto :heraldry.motto.type/motto]
   [:string.entity/slogan :heraldry.motto.type/slogan]])

(def type-map
  (options/choices->map type-choices))

(def ^:private type-option
  {:type :option.type/choice
   :choices type-choices
   :ui/label :string.option/type})

(derive :heraldry/motto :heraldry.options/root)

(defmethod interface/options-subscriptions :heraldry/motto [_context]
  #{[:type]
    [:ribbon-variant]})

(defmethod interface/options :heraldry/motto [context]
  (let [ribbon-variant (interface/get-raw-data (c/++ context :ribbon-variant))
        motto-type (interface/get-raw-data (c/++ context :type))]
    (-> {:anchor {:point {:type :option.type/choice
                          :choices (position/anchor-choices
                                    [:top
                                     :bottom])
                          :default (case motto-type
                                     :heraldry.motto.type/slogan :top
                                     :bottom)
                          :ui/label :string.option/point}
                  :offset-x {:type :option.type/range
                             :min -100
                             :max 100
                             :default 0
                             :ui/label :string.option/offset-x
                             :ui/step 0.1}
                  :offset-y {:type :option.type/range
                             :min -100
                             :max 100
                             :default 0
                             :ui/label :string.option/offset-y
                             :ui/step 0.1}
                  :ui/label :string.option/anchor
                  :ui/element :ui.element/position}

         :geometry {:size {:type :option.type/range
                           :min 5
                           :max 300
                           :default 100
                           :ui/label :string.option/size
                           :ui/step 0.1}
                    :ui/label :string.option/geometry
                    :ui/element :ui.element/geometry}

         :ribbon-variant {:ui/label :string.entity/ribbon
                          :ui/element :ui.element/ribbon-reference-select}

         :tincture-foreground {:type :option.type/choice
                               :choices tinctures-without-furs
                               :default :argent
                               :ui/label (ribbon/segment-type-map :heraldry.ribbon.segment.type/foreground)
                               :ui/element :ui.element/tincture-select}

         :tincture-background {:type :option.type/choice
                               :choices (assoc tinctures-without-furs 0 [:string.option.tincture-background-group/other-or-metal
                                                                         [:string.option.tincture-background-choice/none :none]
                                                                         [(tincture/tincture-map :argent) :argent]
                                                                         [(tincture/tincture-map :or) :or]])
                               :default :none
                               :ui/label (ribbon/segment-type-map :heraldry.ribbon.segment.type/background)
                               :ui/element :ui.element/tincture-select}

         :tincture-text {:type :option.type/choice
                         :choices tinctures-without-furs
                         :default :helmet-dark
                         :ui/label (ribbon/segment-type-map :heraldry.ribbon.segment.type/foreground-with-text)
                         :ui/element :ui.element/tincture-select}}
        (cond->
          ribbon-variant (assoc :ribbon (ribbon/options (c/++ context :ribbon))))
        (assoc :type type-option))))
