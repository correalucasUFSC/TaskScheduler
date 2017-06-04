package br.ufsc.inf.ine5611.converters.scheduled.impl;

import br.ufsc.inf.ine5611.converters.Converter;
import br.ufsc.inf.ine5611.converters.scheduled.ConverterTask;
import br.ufsc.inf.ine5611.converters.scheduled.Priority;
import br.ufsc.inf.ine5611.converters.scheduled.ScheduledConverter;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PriorityScheduledConverter implements ScheduledConverter, Comparator<ScheduledConverterTask> {
    public static final int DEFAULT_QUANTUM_LOW = 50;
    public static final int DEFAULT_QUANTUM_NORMAL = 100;
    public static final int DEFAULT_QUANTUM_HIGH = 200;
    public static long contador = 1;
    public Comparator<ScheduledConverterTask> comparador;
    public PriorityBlockingQueue queue = new PriorityBlockingQueue(11, this);
    
    public static HashMap<Priority, Integer> quanta = new HashMap<>();
    public Converter converter;
    public static ScheduledConverterTask tarefaAtual;

    public PriorityScheduledConverter(Converter converter) {
        //TODO implementar
        /* - Salve converter como um field, para uso posterior
           - Registre um listener em converter.addCompletionListener() para que você saiba
         *   quando uma tarefa terminou */
        this.converter = converter;
        this.converter.addCompletionListener(this::removeDaFila);
        
    }
    
    private void removeDaFila(ConverterTask converterTask){
        ScheduledConverterTask task = (ScheduledConverterTask) converterTask;
        task.complete(null);
        queue.remove(converterTask);
    }

    @Override
    public void setQuantum(Priority priority, int milliseconds) {
        /* Dica: use um HasMap<Priority, Integer> para manter os quanta configurados para
         * cada prioridade */
        //TODO implementar
        quanta.put(priority, milliseconds);
    }

    @Override
    public int getQuantum(Priority priority) {
        /* Veja setQuantum */
        //TODO implementar
        return quanta.get(priority);
    }

    @Override
    public Collection<ConverterTask> getAllTasks() {
        /* Junte todas as tarefas não completas em um Collection */
        //TODO implementar
        Collection<ConverterTask> queueCollection = queue;
        ArrayList<ConverterTask> retorno = new ArrayList<>();
        for(ConverterTask tarefa : queueCollection){
            if(!tarefa.isDone()){
                retorno.add(tarefa);
            }
        }
        return retorno;
    } 
    
    private void cancel(ConverterTask convertTask){
        queue.remove(convertTask);
    }

    @Override
    public synchronized ConverterTask convert(InputStream inputStream, OutputStream outputStream,
                                String mediaType, long inputBytes, Priority priority) {
        /* - Crie um objeto ScheduledConverterTask utilizando os parâmetros dessa chamada
         * - Adicione o objeto em alguma fila (é possível implementar com uma ou várias filas)
         * - Se a nova tarefa for mais prioritária que a atualmente executando, interrompa */
        //TODO implementar
        ScheduledConverterTask novaTask = new ScheduledConverterTask(inputStream, 
                outputStream, this::cancel, mediaType, inputBytes, priority, contador++);
        queue.add(novaTask);
        ScheduledConverterTask tarefaPrioritaria = (ScheduledConverterTask) queue.peek();
        if(tarefaPrioritaria != tarefaAtual){
            converter.interrupt();
            //tarefaAtual = tarefaPrioritaria;
            //this.processFor(getQuantum(tarefaAtual.getPriority()), MILLISECONDS);
        }
        return novaTask;
    }

    @Override
    public void processFor(long interval, TimeUnit timeUnit) throws InterruptedException {
        /* Pseudocódigo:
        * interval = tempo_estourado
         * while (!tempo_estourado) {
         *   t = escolha_tarefa();
         *   t.incCycles();
         *   this.converter.processFor(t, getQuantum(t.getPriority()), timeUnit);
         * }
         */
        //TODO implementar
        Stopwatch watch = Stopwatch.createStarted();
        while (watch.elapsed(timeUnit) <= interval) {
            ScheduledConverterTask task = (ScheduledConverterTask) queue.take();
            task.incCycles();
            queue.add(task);
            try {
                this.converter.processFor(tarefaAtual, getQuantum(tarefaAtual.getPriority()), timeUnit);
            } catch (IOException ex) {
                tarefaAtual.completeExceptionally(ex);
                queue.remove(tarefaAtual);
            }
        }            
                    
    }

    @Override
    public synchronized void close() throws Exception {
        /* - Libere quaisquer recursos alocados
         * - Cancele as tarefas não concluídas
         */
        //TODO implementar
    }

    @Override
    public int compare(ScheduledConverterTask task1, ScheduledConverterTask task2) {
        //-1 == prioridade task nova é maior que prioridade task atual
        //0 == prioridade task atual é igual a prioridade task nova
        //1 == prioridade task atual é maior que prioridade task atual
        Priority prioridadeTask1 = task1.getPriority();
        Priority prioridadeTask2 = task2.getPriority();
        if(prioridadeTask1.equals(Priority.NORMAL) && prioridadeTask2.equals(Priority.HIGH)){
            return -1;
        }
        else if(prioridadeTask1.equals(Priority.LOW) && prioridadeTask2.equals(Priority.HIGH)){
            return -1;
        }
        else if(prioridadeTask1.equals(Priority.LOW) && prioridadeTask2.equals(Priority.NORMAL)){
            return -1;
        }
        else if(prioridadeTask1.equals(prioridadeTask2)){
            if(prioridadeTask1.equals(Priority.LOW)){
                if(task1.getInputBytes() == task2.getInputBytes()){
                    int retorno = comparaPrioridadePorCiclos(task1.getCycles(), task2.getCycles());
                    return retorno;
                }
                else if(task1.getInputBytes() > task2.getInputBytes()){
                    return 1;
                }
                else if(task2.getInputBytes() > task1.getInputBytes()){
                    return -1;
                }
            }
            else if(prioridadeTask1.equals(Priority.NORMAL)){
                int retorno = comparaPrioridadePorCiclos(task1.getCycles(), task2.getCycles());
                return retorno;
            }
        }
        return 1;
    }
    
    public int comparaPrioridadePorCiclos(long ciclosTask1, long ciclosTask2){
        int retorno = 0;
        if(ciclosTask1 > ciclosTask2){
            retorno = 1;
        }
        else if(ciclosTask2 > ciclosTask1){
            retorno = -1;
        }
        return retorno;
    }
}
