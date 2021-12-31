(ns heraldry.render-options
  (:require
   [heraldry.coat-of-arms.escutcheon :as escutcheon]
   [heraldry.coat-of-arms.texture :as texture]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]
   [heraldry.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry.component/render-options [_context]
  #{[:escutcheon]
    [:mode]
    [:texture]})

(defmethod interface/options :heraldry.component/render-options [context]
  (let [mode (-> context (c/++ :mode) interface/get-raw-data (or :colours))
        texture (-> context (c/++ :texture) interface/get-raw-data (or :none))
        escutcheon-option (if (-> context :path (= [:collection-form :render-options]))
                            {:type :choice
                             :choices escutcheon/choices
                             :default :none
                             :ui {:label :string.render-options/escutcheon
                                  :form-type :escutcheon-select}}
                            {:type :choice
                             :choices (drop 1 escutcheon/choices)
                             :default :heater
                             :ui {:label :string.render-options/escutcheon
                                  :form-type :escutcheon-select}})
        escutcheon (-> context (c/++ :escutcheon) interface/get-raw-data
                       (or (-> escutcheon-option :choices first second)))]
    (cond-> {:escutcheon escutcheon-option

             :mode {:type :choice
                    :choices [[:string.render-options.mode-choice/colours :colours]
                              [:string.render-options.mode-choice/catching :hatching]]
                    :default :colours
                    :ui {:label :string.render-options/mode
                         :form-type :radio-select}}

             :texture {:type :choice
                       :choices texture/choices
                       :default :none
                       :ui {:label :string.render-options/texture}}

             :shiny? {:type :boolean
                      :default false
                      :ui {:label :string.render-options/shiny?}}

             :escutcheon-shadow? {:type :boolean
                                  :default false
                                  :ui {:label :string.render-options/escutcheon-shadow?}}

             :escutcheon-outline? {:type :boolean
                                   :default false
                                   :ui {:label :string.render-options/escutcheon-outline?}}

             :outline? {:type :boolean
                        :default false
                        :ui {:label :string.render-options/outline?}}

             :squiggly? {:type :boolean
                         :default false
                         :ui {:label :string.render-options/squiggly?}}

             :preview-original? {:type :boolean
                                 :ui {:label :string.render-options/preview-original?}}
             :coat-of-arms-angle {:type :range
                                  :default 0
                                  :min -45
                                  :max 45
                                  :ui {:label :string.render-options/coat-of-arms-angle
                                       :additional-values [[:string.render-options.coat-of-arms-angle-presets/half 22.5]
                                                           [:string.render-options.coat-of-arms-angle-presets/two-thirds 30]
                                                           [:string.render-options.coat-of-arms-angle-presets/full 45]]
                                       :step 1}}
             :scope {:type :choice
                     :choices [[:string.render-options.scope-choice/achievement :achievement]
                               [:string.render-options.scope-choice/coat-of-arms-and-helm :coat-of-arms-and-helm]
                               [:string.render-options.scope-choice/coat-of-arms :coat-of-arms]]
                     :default :achievement
                     :ui {:label :string.render-options/scope}}}

      (= escutcheon :flag) (merge escutcheon/flag-options)

      (not= texture :none) (assoc :texture-displacement?
                                  {:type :boolean
                                   :default false
                                   :ui {:label :string.render-options/simulate-surface}})

      (= mode :colours) (assoc :theme {:type :choice
                                       :choices tincture/theme-choices
                                       :default tincture/default-theme
                                       :ui {:label :string.render-options/theme
                                            :form-type :theme-select}}))))
