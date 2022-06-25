(ns heraldicon.frontend.ui.element.blazonry-editor.editor
  (:require
   ["draft-js" :as draft-js]
   [clojure.string :as s]
   [heraldicon.frontend.auto-complete :as auto-complete]
   [heraldicon.frontend.debounce :as debounce]
   [heraldicon.frontend.ui.element.blazonry-editor.dom :as dom]
   [heraldicon.frontend.ui.element.blazonry-editor.editor-state :as editor-state]
   [heraldicon.frontend.ui.element.blazonry-editor.parser :as parser]
   [heraldicon.frontend.ui.element.blazonry-editor.shared :as shared]
   [heraldicon.frontend.ui.element.blazonry-editor.suggestions :as suggestions]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(def ^:private editor-state-path
  (conj shared/blazonry-editor-path :editor-state))

(def ^:private last-parsed-path
  (conj shared/blazonry-editor-path :last-parsed))

(def ^:private hdn-path
  (conj shared/blazonry-editor-path :hdn))

(rf/reg-event-fx ::clear
  (fn [{:keys [db]} _]
    {:db (-> db
             (assoc-in editor-state-path (editor-state/create))
             (assoc-in last-parsed-path nil))
     :dispatch [::parser/clear-status]}))

(rf/reg-event-fx ::parse-if-changed
  (fn [{:keys [db]} _]
    (let [state (get-in db editor-state-path)
          text (editor-state/text state)
          last-parsed (get-in db last-parsed-path)]
      (if (not= text last-parsed)
        {:dispatch [::parser/parse text]}
        {}))))

(def ^:private change-dedupe-time
  250)

(rf/reg-event-fx ::update-editor-state
  (fn [{:keys [db]} [_ state]]
    {:db (assoc-in db editor-state-path state)
     ::debounce/dispatch [::debounce-update-editor-state [::parse-if-changed] change-dedupe-time]}))

(rf/reg-event-fx ::update-editor-state-and-parse
  (fn [{:keys [db]} [_ state]]
    {:db (assoc-in db editor-state-path state)
     :dispatch [::parse-if-changed]}))

(rf/reg-event-fx ::set-blazon
  (fn [{:keys [db]} [_ blazon]]
    (let [state (get-in db editor-state-path)]
      {:dispatch [::update-editor-state-and-parse (editor-state/set-text state blazon)]})))

(rf/reg-event-fx ::auto-completion-clicked
  (fn [{:keys [db]} [_ index cursor-index choice]]
    (let [state (get-in db editor-state-path)]
      {:dispatch-n [[::auto-complete/clear]
                    [::update-editor-state-and-parse (editor-state/replace-text state index cursor-index choice)]]})))

(rf/reg-event-fx ::on-parse-result
  (fn [{:keys [db]} [_ text {:keys [hdn index suggestions]}]]
    (let [state (get-in db editor-state-path)
          cursor-index (-> (editor-state/cursor-index state)
                           (max index)
                           (min (count text)))
          substring-since-error (s/trim (subs text index cursor-index))]
      {:db (-> db
               (update-in editor-state-path editor-state/highlight-unknown-string index)
               (assoc-in last-parsed-path text)
               (cond->
                 hdn (assoc-in (conj hdn-path :coat-of-arms) {:field hdn})))
       :dispatch [::suggestions/set (dom/caret-position index) suggestions substring-since-error
                  (fn [choice]
                    (rf/dispatch [::auto-completion-clicked index cursor-index choice]))]})))

(defn editor []
  [:div {:style {:display "inline-block"
                 :outline "1px solid black"
                 :width "100%"
                 :height "100%"}}
   [(r/create-class
     {:display-name "core"
      :reagent-render (fn []
                        (let [editor-state @(rf/subscribe [:get editor-state-path])]
                          [:> draft-js/Editor
                           {:editorState (:state editor-state)
                            :onChange #(rf/dispatch-sync [::update-editor-state (editor-state/EditorState. %)])
                            :keyBindingFn (fn [event]
                                            (if (= (.-code event) "Tab")
                                              (do
                                                (.preventDefault event)
                                                (.stopPropagation event)
                                                "auto-complete")
                                              (draft-js/getDefaultKeyBinding event)))
                            :handleKeyCommand (fn [command]
                                                (if (= command "auto-complete")
                                                  (do
                                                    (rf/dispatch [::auto-complete/apply-first])
                                                    "handled")
                                                  "not-handled"))}]))})]])
