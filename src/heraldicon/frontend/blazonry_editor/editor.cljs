(ns heraldicon.frontend.blazonry-editor.editor
  (:require
   [clojure.string :as str]
   [heraldicon.frontend.auto-complete :as auto-complete]
   [heraldicon.frontend.blazonry-editor.dom :as dom]
   [heraldicon.frontend.blazonry-editor.parser :as parser]
   [heraldicon.frontend.blazonry-editor.shared :as shared]
   [heraldicon.frontend.blazonry-editor.state :as state]
   [heraldicon.frontend.blazonry-editor.suggestions :as suggestions]
   [heraldicon.frontend.debounce :as debounce]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(def ^:private state-path
  (conj shared/blazonry-editor-path :state))

(def ^:private last-parsed-path
  (conj shared/blazonry-editor-path :last-parsed))

(def ^:private hdn-path
  (conj shared/blazonry-editor-path :hdn))

(rf/reg-event-fx ::clear
  (fn [{:keys [db]} _]
    {:db (-> db
             (assoc-in state-path (state/create))
             (assoc-in last-parsed-path nil))
     :dispatch [::parser/clear-status]}))

(rf/reg-event-fx ::parse-if-changed
  (fn [{:keys [db]} _]
    (let [state (get-in db state-path)
          text (state/text state)
          last-parsed (get-in db last-parsed-path)]
      (if (not= text last-parsed)
        {:dispatch [::parser/parse text]}
        {}))))

(def ^:private change-dedupe-time
  250)

(rf/reg-event-fx ::update-text
  (fn [{:keys [db]} [_ text cursor]]
    {:db (update-in db state-path state/update-from-input text cursor)
     ::debounce/dispatch [::debounce-update-state [::parse-if-changed] change-dedupe-time]}))

(rf/reg-event-db ::set-cursor
  (fn [db [_ cursor]]
    (update-in db state-path state/set-cursor-index cursor)))

(rf/reg-event-fx ::update-state-and-parse
  (fn [{:keys [db]} [_ state]]
    {:db (assoc-in db state-path state)
     :dispatch [::parse-if-changed]}))

(rf/reg-event-fx ::set-blazon
  (fn [{:keys [db]} [_ blazon]]
    (let [state (get-in db state-path)]
      {:dispatch [::update-state-and-parse (state/set-text state blazon)]})))

(rf/reg-event-fx ::auto-completion-clicked
  (fn [{:keys [db]} [_ index cursor-index choice]]
    (let [state (get-in db state-path)]
      {:dispatch-n [[::auto-complete/clear]
                    [::update-state-and-parse (state/replace-text state index cursor-index choice)]]})))

(rf/reg-event-fx ::on-parse-result
  (fn [{:keys [db]} [_ text {:keys [hdn index suggestions]}]]
    (let [state (get-in db state-path)
          cursor-index (-> (state/cursor-index state)
                           (max index)
                           (min (count text)))
          substring-since-error (str/trim (subs text index cursor-index))]
      {:db (-> db
               (update-in state-path state/highlight-unknown-string index)
               (assoc-in last-parsed-path text)
               (cond->
                 hdn (assoc-in (conj hdn-path :coat-of-arms) {:type :heraldry/coat-of-arms
                                                              :field hdn})))
       :dispatch [::suggestions/set (dom/caret-position index) suggestions substring-since-error
                  (fn [choice]
                    (rf/dispatch [::auto-completion-clicked index cursor-index choice]))]})))

(defn- highlighted-text
  "The text for the layer behind the textarea, with the unparsed rest in red."
  [text error-index]
  (let [error-index (some-> error-index (min (count text)))]
    [:<>
     (if error-index
       (subs text 0 error-index)
       text)
     (when error-index
       [:span.blazonry-editor-error (subs text error-index)])
     ;; a trailing newline only takes up a line if something follows it
     "​"]))

;; The textarea shows a caret but transparent text; the identically styled layer
;; behind it shows the text with the highlighting. The textarea is uncontrolled:
;; a controlled one loses keystrokes when a render with a stale value arrives
;; while typing. Only changes made by the app are written into it.
(defn editor []
  (let [input (atom nil)
        backdrop (atom nil)
        rendered-state (atom nil)
        applied-revision (atom nil)
        apply-app-changes! (fn []
                             (when-let [^js element @input]
                               (let [state @rendered-state
                                     revision (state/revision state)]
                                 (when (not= revision @applied-revision)
                                   (reset! applied-revision revision)
                                   (when (not= (.-value element) (state/text state))
                                     (set! (.-value element) (state/text state)))
                                   (let [cursor (state/cursor-index state)]
                                     (.focus element)
                                     (.setSelectionRange element cursor cursor))))))]
    (r/create-class
     {:display-name "blazonry-editor"
      :component-did-mount #(reset! applied-revision (state/revision @rendered-state))
      :component-did-update apply-app-changes!
      :reagent-render
      (fn []
        (let [state @(rf/subscribe [:get state-path])
              text (state/text state)]
          (reset! rendered-state state)
          [:div.blazonry-editor
           [:div.blazonry-editor-text.blazonry-editor-backdrop
            {:ref #(reset! backdrop %)}
            [highlighted-text text (state/error-index state)]]
           [:textarea.blazonry-editor-text.blazonry-editor-input
            {:id dom/input-id
             :ref #(reset! input %)
             :default-value text
             :spell-check false
             :auto-complete "off"
             :auto-capitalize "off"
             :auto-correct "off"
             :on-change (fn [^js event]
                          (let [element (.-target event)]
                            (rf/dispatch-sync [::update-text (.-value element) (.-selectionStart element)])))
             :on-select (fn [^js event]
                          (rf/dispatch-sync [::set-cursor (.. event -target -selectionStart)]))
             :on-scroll (fn [^js event]
                          (when-let [^js element @backdrop]
                            (set! (.-scrollTop element) (.. event -target -scrollTop))))
             :on-key-down (fn [^js event]
                            (when (= (.-key event) "Tab")
                              (.preventDefault event)
                              (.stopPropagation event)
                              (rf/dispatch [::auto-complete/apply-first])))}]]))})))
