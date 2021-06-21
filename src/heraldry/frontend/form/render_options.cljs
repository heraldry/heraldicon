(ns heraldry.frontend.form.render-options
  (:require [heraldry.coat-of-arms.texture :as texture]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.escutcheon :as escutcheon]
            [heraldry.frontend.form.state]
            [heraldry.frontend.form.theme :as theme]
            [re-frame.core :as rf]))

(defn form [db-path]
  [element/component db-path :render-options "Options" nil
   (let [mode-path (conj db-path :mode)
         outline-path (conj db-path :outline?)]
     [:<>
      [escutcheon/form (conj db-path :escutcheon-override) "Escutcheon Override"
       :label-width "11em"
       :allow-none? true]
      [element/radio-select mode-path [["Colours" :colours]
                                       ["Hatching" :hatching]]
       :default :colours
       :on-change #(let [new-mode %]
                     (rf/dispatch [:set mode-path new-mode])
                     (case new-mode
                       :hatching (rf/dispatch [:set outline-path true])
                       :colours (rf/dispatch [:set outline-path false])))]
      (when (= @(rf/subscribe [:get mode-path]) :colours)
        [theme/form (conj db-path :theme)])])
   [element/select (conj db-path :texture) "Texture" texture/choices]
   (when (-> @(rf/subscribe [:get (conj db-path :texture)])
             (or :none)
             (#(when (not= % :none) %)))
     [element/checkbox (conj db-path :texture-displacement?) "Apply texture"])
   [element/checkbox (conj db-path :shiny?) "Shiny"]
   [element/checkbox (conj db-path :escutcheon-shadow?) "Escutcheon shadow (ignored for export)"]
   [element/checkbox (conj db-path :escutcheon-outline?) "Escutcheon outline"]
   [element/checkbox (conj db-path :outline?) "Draw outline"]
   [element/checkbox (conj db-path :squiggly?) "Squiggly lines (can be slow)"]])
