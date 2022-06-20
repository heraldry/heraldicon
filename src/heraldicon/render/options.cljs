(ns heraldicon.render.options
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.heraldry.escutcheon :as escutcheon]
   [heraldicon.interface :as interface]
   [heraldicon.render.mode :as mode]
   [heraldicon.render.scope :as scope]
   [heraldicon.render.texture :as texture]
   [heraldicon.render.theme :as theme]))

(derive :heraldry/render-options :heraldry.options/root)

(defmethod interface/options-subscriptions :heraldry/render-options [_context]
  #{[:escutcheon]
    [:mode]
    [:texture]})

(defmethod interface/options :heraldry/render-options [context]
  (let [mode (-> context (c/++ :mode) interface/get-raw-data (or :colours))
        texture (-> context (c/++ :texture) interface/get-raw-data (or :none))
        ;; TODO: path shouldn't be hard-coded
        escutcheon-option (if (-> context :path (= (conj (form/data-path :heraldicon.entity/collection) :render-options)))
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
                    :choices mode/choices
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
                     :choices scope/choices
                     :default :achievement
                     :ui {:label :string.render-options/scope}}}

      (= escutcheon :flag) (merge escutcheon/flag-options)

      (not= texture :none) (assoc :texture-displacement?
                                  {:type :boolean
                                   :default false
                                   :ui {:label :string.render-options/simulate-surface}})

      (= mode :colours) (assoc :theme {:type :choice
                                       :choices theme/choices
                                       :default theme/default
                                       :ui {:label :string.render-options/theme
                                            :form-type :theme-select}}))))
