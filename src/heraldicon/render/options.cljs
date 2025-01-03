(ns heraldicon.render.options
  (:require
   [heraldicon.heraldry.escutcheon :as escutcheon]
   [heraldicon.render.mode :as mode]
   [heraldicon.render.scope :as scope]
   [heraldicon.render.texture :as texture]
   [heraldicon.render.theme :as theme]))

(derive :heraldry/render-options :heraldry.options/root)

(defn build [{:keys [mode texture with-root-escutcheon? escutcheon]}]
  (let [mode (or mode :colours)
        texture (or texture :none)
        ;; TODO: path shouldn't be hard-coded
        escutcheon-option (if with-root-escutcheon?
                            {:type :option.type/choice
                             :choices escutcheon/choices
                             :default :none
                             :ui/label :string.render-options/escutcheon
                             :ui/element :ui.element/escutcheon-select}
                            {:type :option.type/choice
                             :choices (drop 1 escutcheon/choices)
                             :default :heater
                             :ui/label :string.render-options/escutcheon
                             :ui/element :ui.element/escutcheon-select})
        escutcheon (or escutcheon (-> escutcheon-option :choices first second))]
    (cond-> {:escutcheon escutcheon-option

             :mode {:type :option.type/choice
                    :choices mode/choices
                    :default :colours
                    :ui/label :string.render-options/mode
                    :ui/element :ui.element/radio-select}

             :texture {:type :option.type/choice
                       :choices texture/choices
                       :default :none
                       :ui/label :string.render-options/texture}

             :border? {:type :option.type/boolean
                       :default true
                       :ui/label :string.render-options/border?}

             :shiny? {:type :option.type/boolean
                      :default false
                      :ui/label :string.render-options/shiny?}

             :escutcheon-shadow? {:type :option.type/boolean
                                  :default false
                                  :ui/label :string.render-options/escutcheon-shadow?}

             :escutcheon-outline? {:type :option.type/boolean
                                   :default false
                                   :ui/label :string.render-options/escutcheon-outline?}

             :outline? {:type :option.type/boolean
                        :default false
                        :ui/label :string.render-options/outline?}

             :squiggly? {:type :option.type/boolean
                         :default false
                         :ui/label :string.render-options/squiggly?}

             :coat-of-arms-angle {:type :option.type/range
                                  :default 0
                                  :min -45
                                  :max 45
                                  :ui/label :string.render-options/coat-of-arms-angle
                                  :ui/additional-values [[:string.render-options.coat-of-arms-angle-presets/half 22.5]
                                                         [:string.render-options.coat-of-arms-angle-presets/two-thirds 30]
                                                         [:string.render-options.coat-of-arms-angle-presets/full 45]]
                                  :ui/step 1}
             :scope {:type :option.type/choice
                     :choices scope/choices
                     :default :achievement
                     :ui/label :string.render-options/scope}}

      (= escutcheon :flag) (merge escutcheon/flag-options)

      (not= texture :none) (assoc :texture-displacement?
                                  {:type :option.type/boolean
                                   :default false
                                   :ui/label :string.render-options/simulate-surface})

      (= mode :colours) (assoc :theme {:type :option.type/choice
                                       :choices theme/choices
                                       :default theme/default
                                       :ui/label :string.render-options/theme
                                       :ui/element :ui.element/theme-select}))))
